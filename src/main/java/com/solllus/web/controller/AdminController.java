package com.solllus.web.controller;

import com.boosteel.nativedb.NativeDB;
import com.boosteel.util.explorer.IOManager;
import com.solllus.web.controller.domain.*;
import com.solllus.web.controller.utils.ContentManager;
import com.solllus.web.controller.utils.DB;
import com.solllus.web.security.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.solllus.web.controller.support.StaticVariable.*;


/*
 *  /data/user/..../{user}
 *  user로 시작하는 URL은 로그인한 사용자가 자신의 컨텐츠만 접근할 수 있도록 인터셉터가 관여한다.
 *  맨 마지막 paths는 해당하는 user의 아이디가 들어가야 한다.ㅡ
 *
 */
@Controller
@RequestMapping("data")
public class AdminController {


    @Autowired
    private NativeDB nativeDB;
    private DB db;


    // 각 고객정보

    @PostConstruct
    public void before() throws Exception {

        this.db = new DB(nativeDB);


        loadUsers();
        loadApps();

        // 접속 키 로딩
        for (DisplayKey key : db.getKeys()) {
            DISPLAY_KEYS.put(key.value, key);
        }

    }

    @RequestMapping(value = "i/root")
    @ResponseBody
    public String rootPath() {
        return ROOT.toString().replaceAll("\\\\", "/");
    }


    /* *************************** ▼ 로그인 ▼ *************************** */
    private UserDetail getUserDetail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetail) return (UserDetail) principal;
        return null;
    }

    // admin과 app 페이지에서 이름을 받아간다.
    @RequestMapping(value = "i/name", method = RequestMethod.POST)
    @ResponseBody
    public Object getId() {
        UserDetail user = getUserDetail();
        if (user == null) return new Object[0];
        return new Object[]{user.getUsername(), user.getAuthorities().stream().map(v -> v.getAuthority()).collect(Collectors.toSet())};
    }

    @RequestMapping(value = "i/has/{user}")
    @ResponseBody
    public boolean hasUser(@PathVariable("user") String user) {
        return USERS.containsKey(user);
    }

    @RequestMapping(value = "i/login/check")
    @ResponseBody
    public boolean loginChk(@RequestBody String key) {
        return USERS.keySet().contains(key);
    }

    @RequestMapping(value = "i/login/pass", method = RequestMethod.POST)
    @ResponseBody
    public boolean loginPass(@RequestBody String info) {
        String[] values = info.split("\n");
        String name = values[0],
                pass = values[1];
        UserDetail userDetail = USERS.get(name).userDetail;
        if (userDetail.getPassword().equals(pass)) {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(userDetail, userDetail.getPassword(), userDetail.getAuthorities()));
            return true;
        }
        return false;
    }
    /* *************************** ▲ 로그인 ▲ *************************** */


    /* *************************** ▼ 메인페이지 접속키 ▼ *************************** */
    private String uuid() {
        return UUID.randomUUID().toString().split("-")[0].substring(0, 4);
    }

    @RequestMapping(value = "i/certify/create")
    @ResponseBody
    public String certify() {
        return new SimpleDateFormat("yyMMdd").format(new Date()) + "-" + uuid();
    }

    @RequestMapping(value = "i/certify")
    @ResponseBody
    public Object certifyCheck(@RequestParam("value") String value,
                               @RequestParam("name") String name,
                               @RequestParam("index") String index) {
        DisplayKey exist = DISPLAY_KEYS.get(value);
        UserDetail userDetail = getUserDetail();
        if (userDetail != null) {
            if (userDetail.roles.contains("ADMIN")) return "ADMIN-" + uuid();
            if (userDetail.getUsername().equals(name)) return name;
        }
        if (exist == null || !exist.equals(name, index)) return "";
        return value;
    }

    @RequestMapping(value = "i/certify", method = RequestMethod.PUT)
    @ResponseBody
    public void certifySave(@RequestBody DisplayKey key) {
        DisplayKey exist = DISPLAY_KEYS.get(key.value);
        if (exist == null) {
            db.addKey(key.createTime());
            DISPLAY_KEYS.put(key.value, key);
        } else {
            db.modifyKey(exist.extend(key));
        }
    }

    @RequestMapping(value = "i/certify", method = RequestMethod.DELETE)
    @ResponseBody
    public void certifySave(@RequestParam("value") String value) {
        db.deleteKey(value);
        DISPLAY_KEYS.remove(value);
    }
    /* *************************** ▲ 메인페이지 접속키 ▲ *************************** */


    /* ******************************** ★★★   ▼디스플레이 정보▼   ★★★ ******************************** */

    /*
     * 이 맵핑은 폴링방식에 대응한다.
     * 폴링방식의 과도한 데이터 전송을 막기 위해,
     * 브라우저에게서 displayTime과 ContentTime을 받아 이를 비교한뒤 변경이 있을때만 데이터를 반환한다.
     */
    @RequestMapping(value = "i/{path}/{display}", method = RequestMethod.POST)
    @ResponseBody
    public Display pollingAccept(@PathVariable("path") String path, @PathVariable("display") String index,
                                 @RequestBody long[] times) {
        Display display = USERS.get(path).getDisplay(index);
        if (display == null) return null;
        Content content = display.getContent();
        if (content == null) return null;

        long currentDisplayTime = times[0], currentContentTime = times[1],
                displayTime = display.updateTime, contentTime = content.time;

        // 변경사항이 있을 경우에만 info를 반환한다.
        if (currentDisplayTime != displayTime || currentContentTime != contentTime) return display;
        return null;
    }


    // SSE
    public static class CustomEmitter extends SseEmitter {
        public String certifyKey;       // 인증키
        public String index;            // 디스플레이 번호
        public String user;     // path :: username
        public String host;
        public Display display;
        public String createTime_kr;
        public long createTime;

        public long sendTime = new Date().getTime();   // 계속 덮어씌어지며 실제 변경시간만 복사되어 이어진다. 로직을 잘 살펴볼것

        public CustomEmitter(long timeout) {
            super(timeout);
            Date date = new Date();
            createTime_kr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
            createTime = date.getTime();
        }

        CustomEmitter set(String index, String user, String host) {
            this.index = index;
            this.user = user;
            this.host = host;
            return this;
        }

        CustomEmitter setDisplay(Display display) {
            this.display = display;
            return this;
        }

        CustomEmitter setCertifyKey(String certifyKey) {
            this.certifyKey = certifyKey;
            return this;
        }

        public CustomEmitter setSendTime() {
            return this.setSendTime(new Date().getTime());
        }

        public CustomEmitter setSendTime(long time) {
            this.sendTime = time;
            return this;
        }

        public int getHash() {
            return hashCode();
        }

        @Override
        public void send(Object object, MediaType mediaType) throws IOException {
            setSendTime();
            super.send(object, mediaType);
        }
    }

    private static final Map<String, List<CustomEmitter>> EMITTERS = new ConcurrentHashMap<>();

    // [path : 유저 아이디], 브라우저에서 sse 처음 접속
    @GetMapping(value = "/i/{path}/{display}", params = {"sse", "certifyKey"})
    @ResponseBody
    public SseEmitter subscribe(
            @PathVariable("path") String path,
            @PathVariable("display") String index,
            @RequestParam("certifyKey") String certifyKey,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            @RequestHeader(value = "host", required = false) String host,
            HttpServletResponse response
    ) {
        /*
         * 2023-08-04
         * 가비아는 Nginx 서버를 사용하는데, 이 서버의 버퍼링때문에 SSE가 전혀 동작하지 않는다.
         * 이 버퍼링을 비활성화시키는 헤더를 포함시켜야 한다.
         * 이거땜에 가비아 컨테이너 호스팅 셋팅해놓고 이틀간 개고생함...
         */
        response.addHeader("X-Accel-Buffering", "no");

        // 등록되지 않은 사용자/디스플레이 경우 접속 해제
        ContentMap_users userMap = PROVIDER("users", path);
        if (userMap == null) return null;
        else if (!userMap.displayMap.containsKey(index)) return null;

        CustomEmitter emitter = new CustomEmitter(10 * 60 * 1000)
                .set(index, path, host)
                .setDisplay(userMap.getDisplay(index))
                .setCertifyKey(certifyKey)
                .setSendTime();

        /*
         * 왜 그런지는 모르겠지만, sse 커넥션 하나당 2개의 emitter가 만들어져서 같이 돌아간다.
         * 모든 sse 커넥션은 고유한 certify를 가지므로, 이를 기준으로 사용하지 않는 커넥션을 직접 삭제한다.
         */
        final List<CustomEmitter> list = emitterList0(path);
        for (CustomEmitter target : list) {
            if (target.certifyKey.equals(certifyKey)) {
                emitter.setSendTime(target.sendTime);
                target.complete();
                list.remove(target);
            }
        }
        list.add(emitter);
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onCompletion(() -> list.remove(emitter));

        // 에러방지용 더미 데이터 전송이므로, sendTime을 갱신하지 않도록 한다.
        long temp = emitter.sendTime;
        send0(emitter);   // 503에러 방지
        emitter.sendTime = temp;
        return emitter;
    }

    private List<CustomEmitter> emitterList0(String user) {
        List<CustomEmitter> list = EMITTERS.get(user);
        if (list == null) EMITTERS.put(user, list = new CopyOnWriteArrayList<>());
        return list;
    }

    private void send0(CustomEmitter emitter) {
        try {
            emitter.send(emitter.display, MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            EMITTERS.get(emitter.user).remove(emitter);
        }
    }

    private List<CustomEmitter> EMPTY_LIST = new ArrayList<>();

    private List<CustomEmitter> getEmitterList(String username) {
        List<CustomEmitter> list = EMITTERS.get(username);
        return list == null ? EMPTY_LIST : new ArrayList<>(list);
    }
    /* ******************************** ★★★   ▲디스플레이 정보▲   ★★★ ******************************** */


    /* ***************************  ▼ ADMIN ▼ *************************** */

    private void loadUsers() throws IOException {
        Set<String> registerUsers = db.users();
        // DB기준으로 User를 로딩한다.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(USERS_DIR)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    String name = path.getFileName().toString();
                    if (registerUsers.contains(name))
                        loadUser(name, path);
                }
            }
        }
    }

    @RequestMapping("/s/user/list")
    @ResponseBody
    public Object userList() {
        return USERS.values().stream().map(s -> s.userDetail).collect(Collectors.toSet());
    }

    @GetMapping(value = "/s/display/all")
    @ResponseBody
    public Object displayAll() {
        return new Object[]{DISPLAY_KEYS, EMITTERS};
    }

    @GetMapping(value = "/s/user/{name}")
    @ResponseBody
    public Object userDetail(@PathVariable("name") String name) {
        return db.userDetail(name);
    }

    @RequestMapping("/s/user/info/{user}")
    @ResponseBody
    public Object info(@PathVariable("user") String user) throws Exception {
        boolean dirExists = Files.exists(USERS_DIR.resolve(user));
        UserDetail userDetail = db.userDetail(user);
        return new Object[]{USERS.containsKey(user), userDetail, dirExists};
    }

    // 유저가 사용중인 리소스 용량
    @GetMapping(value = "/s/user/resources")
    @ResponseBody
    public Object userResources(@RequestParam("name") String name) throws Exception {
        Path userPath = USERS_DIR.resolve(name);
        long[] array = {0, 0};
        if (Files.exists(userPath)) array = sum(userPath, array);
        return array;
    }

    private long[] sum(Path path, long[] array) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path file : stream) sum(file, array);
            }
        } else {
            array[0]++;
            array[1] += Files.size(path);
        }
        return array;
    }

    // 유저 정보는
    // 0 : 미등록
    // 1 : 삭제상태 (복원 옵션)
    // 2 : 등록

    // 유저 추가
    // flag ==>  0 : create / 1 : update
    @RequestMapping(value = "s/user/register/{path}")
    @ResponseBody
    public void register(@PathVariable("path") String path,
                         @RequestParam("pass") String pass,
                         @RequestParam(value = "roles", defaultValue = "") String roles) throws Exception {

        if (!USERS.containsKey(path)) {
            Path dir = USERS_DIR.resolve(path);
            Files.createDirectories(dir.resolve("_AppData"));   // _AppData 폴더도 생성

            db.addUser(path, pass, roles);
            loadUser(path, dir);
        } else {
            USERS.get(path).userDetail.setPassword(pass).setRoles(roles);
            db.updateUser(path, pass, roles);
        }
    }

    public void loadUser(String name, Path path) throws IOException {
        UserDetail userDetail = db.userDetail(name);
        if (userDetail != null) {
            ContentMap_users contentMap = new ContentMap_users(path, "users", name).setUserDetail(userDetail).readAll();
            contentMap.displayMap = db.display(name);
            USERS.put(name, contentMap);

            // 각 디스플레이가 가지고 있는 컨텐츠목록 로딩
            for (Display display : contentMap.displayMap.values())
                loadDisplayContents(name, display.index);
        }
    }

    private Display loadDisplayContents(String name, String index) {
        ContentMap_users users = USERS.get(name);
        Display display = users.displayMap.get(index);
        if (display != null) {
            display.contents = db.getAppList(name, index);
        }
        return display;
    }


    // 유저 삭제 (완전 삭제는 아니다.)
    @RequestMapping(value = "s/user/remove/{name}")
    @ResponseBody
    public int removeUser(@PathVariable("name") String name) {
        // 모든 디스플레이를 끄고 emitter를 갱신한다.
        ContentMap_users map = USERS.remove(name);
        map.displayMap = EMPTY_DISPLAY_MAP;
        EMITTERS.remove(name);
        db.removalUser(name);
        return 1;
    }

    // 유저 복원
    @RequestMapping(value = "s/user/restore/{name}")
    @ResponseBody
    public int restoreUser(@PathVariable("name") String name) throws IOException {
        loadUser(name, USERS_DIR.resolve(name));
        db.restoreUser(name);
        return 1;
    }


    // 유저 완전 삭제
    @RequestMapping(value = "s/user/destroy/{name}")
    @ResponseBody
    public int destroyUser(@PathVariable("name") String name) throws IOException {
        IOManager.deleteAll(USERS_DIR.resolve(name));
        db.destroyUser(name);
        return 1;
    }

    //http://localhost/data/s/display/create/test/1
    // 디스플레이 추가
    @RequestMapping(value = "s/display/create/{path}/{displayIndex}")
    @ResponseBody
    public int addDisplay(@PathVariable("path") String path, @PathVariable("displayIndex") String displayIndex) throws IOException {
        ContentMap_users contentMap = USERS.get(path);
        if (contentMap == null) return -1;
        if (contentMap.displayMap.containsKey(displayIndex)) return 0;

        Display display = new Display(displayIndex, path);
        contentMap.addDisplay(display);
        db.addDisplay(path, display);
        return 1;
    }

    // 디스플레이 삭제
    @RequestMapping(value = "s/display/remove/{path}/{display}")
    @ResponseBody
    public void removeDisplay(@PathVariable("path") String path, @PathVariable("display") String display) throws IOException {
        ContentMap_users contentMap = USERS.get(path);
        contentMap.removeDisplay(display);
        db.removeDisplay(path, display);
        db.removeApps(path, display);
    }


    // 앱 로딩
    @RequestMapping(value = "s/apps/refresh")
    @ResponseBody
    private void loadApps() throws IOException {
        Path rootPath = ROOT.resolve("apps");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            Map<String, ContentMap_apps> newMap = new HashMap<>();
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    String pathName = path.getFileName().toString();
                    ContentMap_apps contentMap = new ContentMap_apps(path, "apps", pathName).readAll();
                    newMap.put(pathName, contentMap);
                }
            }
            APPS = newMap;
        }
    }

    @RequestMapping(value = "s/apps/all")
    @ResponseBody
    private Object appsAll() throws IOException {
        return APPS.values();
    }


    // 앱 설치
    @RequestMapping(value = "s/app/install", method = RequestMethod.POST)
    @ResponseBody
    public void registerApp(@RequestBody ContentManager manager) {
        Display display = USERS.get(manager.user).displayMap.get(manager.index);
        db.addApp(manager.user, manager.index, manager.path());
        display.clearData(manager.root, manager.path, manager.name);
        display.contents.add(manager.path());
    }

    // 앱 삭제
    @RequestMapping(value = "s/app/uninstall", method = RequestMethod.POST)
    @ResponseBody
    public void userRemoveApp(@RequestBody ContentManager manager) {
        Display display = USERS.get(manager.user).displayMap.get(manager.index);
        db.removeApp(manager.user, manager.index, manager.path());
        display.clearData(manager.root, manager.path, manager.name);
        display.contents.remove(manager.path());
    }

    // [공용 앱 삭제] 모든 유저의 앱 목록에서 삭제한다.
    @RequestMapping(value = "app/remove", method = RequestMethod.POST)
    @ResponseBody
    public void removeApp(@RequestBody ContentManager manager) throws IOException {
        db.deleteApp(manager.path());
    }
    /* ***************************  ▲ ADMIN ▲ *************************** */


    /* ***************************  ▼ 디스플레이 정보 ▼ *************************** */
    // 모든 디스플레이 정보
    @RequestMapping(value = "user/display/{path}")
    @ResponseBody
    public Object display(@PathVariable("path") String path) {
        ContentMap_users contentMap = USERS.get(path);
        return contentMap != null ? contentMap.displayMap : new HashMap<>();
    }

    // 해당 디스플레이에서 접근할 수 있는 리소스들
    @RequestMapping(value = "user/display/contents/{path}")
    @ResponseBody
    private Object appsAll(@PathVariable("path") String user, @RequestParam("index") String index) {
        ContentMap_users users = USERS.get(user);
        // {모든 앱, 유저 전용앱, 디스플레이에 등록된 앱}
        return new Object[]{APP_ALL(), users.contents.values(), users.getDisplay(index).contents};
    }

    // 디스플레이별 컨텐츠 파일목록 반환
    @RequestMapping(value = "user/contents/{path}")
    @ResponseBody
    public Object list(@PathVariable("path") String username, @RequestParam("index") String index) {
        return db.getAppList(username, index);
    }
    /* ***************************  ▲ 디스플레이 정보 ▲ *************************** */


    /* ***************************  ▼ 디스플레이 & 컨텐츠 ▼ *************************** */
    // 디스플레이에 컨텐츠 적용
    /*
     * 디스플레이(/v/user/1)는 유저 계정이 소유하는 것이 아니다.
     * 해당 유저 계정이 그 디스플레이에 나타날 컨텐츠를 정의할 뿐이다. (path/name)
     * 따라서 설정할 수 있게만 한다면 다른 유저(폴더)의 컨텐츠도 해당 디스플레이에 나타나게 할 수 있다.
     * 때문에 root, path, name으로만 디스플레이에 컨텐츠를 명시하게 만들었다.
     */
    @RequestMapping(value = "user/update/display/{name}", method = RequestMethod.POST)
    @ResponseBody
    public void setDisplay(@PathVariable("name") String user,
                           @RequestBody ContentManager manager,
                           @RequestParam(name = "command", defaultValue = "1") int command) {
        ContentMap_users contentMap = USERS.get(user);

        if (contentMap != null) {
            String index = manager.index,
                    root = manager.root,
                    path = manager.path,
                    name = manager.name;

            Display display = contentMap.getDisplay(index);
            if (display == null) return;

            // body에 텍스트가 들어온다.
            if (manager.body != null) display.setText(manager.body);
            else display.setContent(root, path, name);      // 컨텐츠 정의

            db.updateDisplay(user, display);

            reloadIndex(user, index, command, null);
        }
    }

    // 앱, 혹은 컨텐츠 변경/삭제/등록 시 호출
    @RequestMapping(value = {"user/update/content/{path}", "update/content"}, method = RequestMethod.POST)
    @ResponseBody
    public int updateContent(@RequestBody ContentManager manager) throws IOException {

        ContentMap map = PROVIDER(manager.root, manager.path);
        Content.register(ROOT.resolve(manager.path()), map.contents);
        return 1;
    }

    // 해당 유저의 컨텐츠의 로컬 변경시간을 일괄 갱신한다.
    @RequestMapping(value = "user/contents/reload/{path}")
    @ResponseBody
    public void refreshContent(@PathVariable("path") String path) throws IOException {
        ContentMap_users contentMap = USERS.get(path);
        contentMap.readAll();
    }
    /* ***************************  ▲ 디스플레이 & 컨텐츠 ▲ *************************** */




    /* ************************* ▼ 외부에서 사용하는 메서드 (디스플레이 강제 새로고침) ▼ ************************* */
    /*
     * command=0 :: 아무것도 하지 않는다.
     * command=1 :: 강제새로고침
     * command=2 :: 데이터 갱신
     */

    /*
     * 해당 컨텐츠를 게시하고 있는 모든 디스플레이를 새로고침한다. (모든 유저 대상)
     * 앱 스토어의 앱을 수정했을 경우, 일괄 적용하기 위해 이 메서드를 사용한다.
     *
     * ?user={user}를 포함하면 해당 유저만 대상으로 한다.
     *
     */
    @RequestMapping(value = "public/reload/content/{root}/{path}/{name}")
    @ResponseBody
    public void reloadContents(@PathVariable("root") String root,
                               @PathVariable("path") String path,
                               @PathVariable("name") String name,
                               @RequestParam(defaultValue = "1") int command,
                               @RequestParam(required = false, name = "request") String request,
                               @RequestParam(required = false) String user) {
        List<CustomEmitter> emitters = new ArrayList<>();

        if (user != null) {
            for (CustomEmitter emitter : getEmitterList(user)) {
                if (emitter != null && emitter.display != null && emitter.display.checkContent(root, path, name)) {
                    emitters.add(emitter);
                }
            }
        } else {
            for (List<CustomEmitter> list : EMITTERS.values()) {
                for (CustomEmitter emitter : new ArrayList<>(list)) {
                    if (emitter != null && emitter.display != null && emitter.display.checkContent(root, path, name)) {
                        emitters.add(emitter);
                    }
                }
            }
        }
        reload0(emitters, command, request);
    }


    /*
     * ★★ 유일하게 계정 사용자 요청에 의해 호출된다. ★★
     * path/{index}에 해당하는 모든 디스플레이를 새로고침한다.
     * 따라서 이 메서드에서 emitter의 lastOrderTime을 갱신한다.
     */
    @RequestMapping(value = "public/reload/display/{user}/{index}")
    @ResponseBody
    public void reloadIndex(@PathVariable("user") String user,
                            @PathVariable("index") String index,
                            @RequestParam(defaultValue = "1") int command,
                            @RequestParam(required = false, name = "request") String request) {
        List<CustomEmitter> emitters = new ArrayList<>();
        for (CustomEmitter emitter : getEmitterList(user)) {
            // 왜인지는 모르겠지만 emitter가 null일 때가 있다.
            if (emitter != null && emitter.display != null && emitter.display.index.equals(index))
                emitters.add(emitter.setSendTime());
        }
        reload0(emitters, command, request);
    }

    // certifyKey에 따라 새로고침
    @RequestMapping(value = "public/reload/certify/{user}/{certify}")
    @ResponseBody
    public void reloadCertify(@PathVariable("user") String user,
                              @PathVariable("certify") String certify,
                              @RequestParam(defaultValue = "1") int command,
                              @RequestParam(required = false, name = "request") String request) {
        List<CustomEmitter> emitters = new ArrayList<>();
        for (CustomEmitter emitter : getEmitterList(user)) {
            // 왜인지는 모르겠지만 emitter가 null일 때가 있다.
            if (emitter != null && emitter.certifyKey.equals(certify))
                emitters.add(emitter);
        }
        reload0(emitters, command, request);
    }


    private void reload0(List<CustomEmitter> list, int command, String request) {
        long now = new Date().getTime();
        for (CustomEmitter emitter : list) {

            Display display = emitter.display;

            if (display != null) {

                display.setCommand(command).setServerTime(now);

                // 요청사항이 있다면 기재
                if (request != null) {
                    display.addRequest(request, now);
                }
                // 강제 새로고침
                if (command == 1) {
                    display.refreshTime = now;
                }
                send0(emitter);
            }
        }
    }
    /* ************************* ▲ 외부에서 사용하는 메서드 ▲ ************************* */


    /* *************************** ▼ 템플릿용 메서드 ▼ *************************** */

    // 폴더 만들기
    @RequestMapping(value = "io/dirs", method = RequestMethod.PUT)
    @ResponseBody
    public void createDirectories(@RequestBody ContentManager manager) throws IOException {
        manager.createDirectories(ROOT);
    }

    // 파일목록 가지고 오기
    @RequestMapping(value = "io/files", method = RequestMethod.POST)
    @ResponseBody
    public Object files(@RequestBody ContentManager manager) throws Exception {
        return manager.files(ROOT);
    }

    // 파일 업로드
    @RequestMapping(value = {"io/files", "user/io/files/{path}"}, method = RequestMethod.PUT)
    @ResponseBody
    public Object filesCopy(@RequestPart("config") ContentManager config, @RequestPart("files") List<MultipartFile> files) throws Exception {
        long size = 0l;
        for (MultipartFile file : files) {
            size += config.copy(ROOT, file.getInputStream(), file.getOriginalFilename());
        }
        return size;
    }

    @RequestMapping(value = {"io/read/file", "public/io/read/file"}, method = RequestMethod.POST)
    @ResponseBody
    public Object readFile(@RequestBody ContentManager config) throws Exception {
        Path p = config.resolve(ROOT);
        if (!Files.exists(p)) return new Object[0];
        return Files.readAllLines(config.resolve(ROOT));
    }

    @RequestMapping(value = {"io/write/file", "user/io/write/file/{path}"}, method = RequestMethod.POST)
    @ResponseBody
    public void writeFile(@RequestBody ContentManager config) throws Exception {
        Path file = config.resolve(ROOT),
                parent = file.getParent();
        if (!Files.exists(parent)) Files.createDirectories(parent);
        Files.write(file, Arrays.asList(config.body), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


    @RequestMapping(value = {"io/files/delete", "user/io/files/delete/{path}"}, method = RequestMethod.POST)
    @ResponseBody
    public Object filesDelete(@RequestBody ContentManager config) throws Exception {
        return config.delete(ROOT);
    }

    @RequestMapping(value = "user/io/delete/temp/{path}", method = RequestMethod.POST)
    @ResponseBody
    public Object filesDeleteTemp(@RequestBody ContentManager config) throws Exception {
        // safe, prefix 필요
        return config.removeTempFiles(ROOT);
    }
    /* *************************** ▲ Template ▲ *************************** */

    private void out(Object... obj) {
        String[] values = new String[obj.length];
        int i = 0;
        for (Object o : obj) values[i++] = o.toString();
        System.out.println(String.join(" ", values));
    }
}
