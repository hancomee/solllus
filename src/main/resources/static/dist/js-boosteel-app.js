const APP = (function () {

    const

        // main/admin 의 <iframe src="/{root}/{path}/{name}/source.html?name={targetPath}&index={num}" ...
        paths = (function () {
            const
                pathname = decodeURI(location.pathname.slice(1)),
                search = decodeURI(location.search.slice(1)),
                [root, path, name] = pathname.split('/'),
                {name: user, index: index} = search.split('&').reduce((r, v) => {
                    const [key, value] = v.split('=');
                    r[key] = value;
                    return r;
                }, {});

            return {
                root: root,
                path: path,
                name: name,
                user: user,
                index: index
            }
        })(),

        {root, path, name, user, index} = paths,

        dataPath = ['users', user, '_AppData', index],
        appName = [root, path, name].join('_'),
        resourcePaths = dataPath.concat(appName),

        configFile = (data) => {
            const value = {paths: dataPath.concat(appName + '.txt')};
            if (data) value.body = JSON.stringify(data);
            return value;
        },

        $ = {

            paths: paths,

            // resources
            src: (filename) => '/' + resourcePaths.concat(filename).join('/'),

            createVideo(sourceSrc) {
                const video = document.createElement('video');
                video.autoplay = video.loop = video.controls = false;
                video.muted = true;
                video.innerHTML = '<source src="' + encodeURI(sourceSrc) + '" type="video/mp4">';
                return video;
            },

            uploadFiles(files) {
                const formData = new FormData();
                forEach.call(files, ({file, filename}) => {
                    formData.append('files', file, filename);
                });
                formData.append('config', JS.bodyBlob({paths: resourcePaths}));
                return JS.fetch('PUT:/data/user/io/files/' + user, formData)
                    .then((res) => res.text());
            },

            removeTemps(safeNames, prefix) {
                return JS.fetch('POST:/data/user/io/delete/temp/' + user, {
                    paths: resourcePaths,
                    safeFiles: Array.isArray(safeNames) ? safeNames : [safeNames],
                    prefix: prefix || ''
                });
            },

            updateContent() {
                return JS.fetch('POST:/data/user/contents/' + user, {root: root, path: path, name: name, user: user, flag: true})
            },
            // then(APP.reloadByContent) 경우 객체가 들어올 수 있다.
            reloadByDisplay() {
                return JS.fetch('/data/public/reload/display/' + [user, index].join('/'));
            },

            reloadByContent(_command = 1) {
                const command = typeof _command === 'number' ? _command : 1;
                return JS.fetch('/data/public/reload/content/' + [root, path, name].join('/') + '?command=' + command + '&user=' + user);
            },
            postMessage(data = {}) {
                return JS.fetch('/data/public/reload/content/' + [root, path, name].join('/') + '?command=0' +
                    '&user=' + user + '&request=' + encodeURI(JSON.stringify(['post', data])));
            },
            setJSON(data) {
                return JS.fetch('POST:/data/user/io/write/file/' + user, configFile(data));
            },
            getJSON() {
                return JS.fetch('POST:/data/public/io/read/file/', configFile())
                    .then(res => res.json())
                    .then(([jsonText]) => jsonText ? JSON.parse(jsonText) : null);
            }
        };

    const [home] = document.getElementsByClassName('home'),
        [referer] = document.getElementsByClassName('referer');
    if(home) {
        home.href = '/admin/' + user;
        home.target = '_parent';
    }
    if(referer) referer.textContent = user + ' / ' + index;

    return $;
})();