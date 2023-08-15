const ADMIN = (function () {

    const
        $$ = {

            getId: () => JS.fetch('POST:/data/i/name').then(res => res.json()),


            getUserInfo: (name) => JS.fetch('/data/s/user?name=' + name).then(res => res.json()),
            getAppList: () => JS.fetch('/data/s/apps/all').then(res => res.json()),

            // 공용앱을 등록중인 user list
            // ==> path name
            getRegisteredList: (config) => JS.fetch('POST:/data/s/app/registered', config).then(res => res.json()),

            createDisplay: (user, displayIndex) => JS.fetch('/data/s/display/create/' + user + '/' + displayIndex).then(res => res.ok),
            deleteDisplay: (user, displayIndex) => JS.fetch('/data/s/display/remove/' + user + '/' + displayIndex).then(res => res.ok),


            // isUpdate :: 0 : create / 1 : update
            createUser: (name, roles, pass, isUpdate) =>
                JS.fetch('/data/s/register/' + name + '?roles=' + roles + '&pass=' + pass + '&flag=' + isUpdate).then(res => res.ok),
            deleteUser: (name) => JS.fetch('/data/s/remove/user?name=' + name).then(res => res.ok),


            // ==> root path name user
            addApp: (config) => JS.fetch('POST:/data/user/app/add', config).then(res => res.ok),
            // ==> root path name user
            removeApp: (config) => JS.fetch('POST:/data/user/app/remove', config).then(res => res.ok),

            // ==> root path(user) name [user]
            updateContent(config) {
                if(config.user) return JS.fetch('POST:/data/user/update/content/' + config.user, config).then(res => res.ok);
                else return JS.fetch('POST:/data/update/content', config).then(res => res.ok);
            },
            // ==> root path name [user] [command]
            reloadByContent({root, path, name, user, command = 1}) {
                const query = ['command=' + command];
                if (user) query.push('user=' + user);
                return JS.fetch('/data/public/reload/content/' + [root, path, name].join('/') + '?' + query.join('&')).then(res => res.ok);
            },

            // ==> index root path user name [command]
            updateDisplay(config) {
                const {command = 1} = config;
                return JS.fetch('POST:/data/user/update/display/' + config.user + '?command=' + command, config).then(res => res.ok);
            },
            // ==> index user [request] [command]
            reloadDisplay({index, user, request, command = 1}) {
                const query = ['command=' + command];
                if (request) query.push('request=' + encodeURI(request));
                return JS.fetch('/data/public/reload/display/' + [user, index].join('/') + '?' + query);
            },

            getDisplays: ($path) => JS.fetch('/data/user/display/' + $path).then(res => res.json()),
            getContents: ($path) => JS.fetch('/data/user/contents/' + $path).then(res => res.json()),

        };


    return $$;

})()