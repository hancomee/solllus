const

    COFFEA = {

        sign(flag) {
            switch (flag) {
                case '@' :
                    return 'best';
                case '#' :
                    return 'soldout';
                case '+' :
                    return 'new';
                default :
                    return '';
            }
        },

        types: {
            menu(lines) {
                const [name, ..._items] = lines.split(/\n/),
                    [, ko, en] = /(.*?)\((.*)\)/.exec(name),
                    items = [];
                _items.forEach(line => {
                    const exec = /([$#@+])?(.*)\((.*)\).*?([\.\d]+).*?([.\d]+)/.exec(line);
                    if (exec) {
                        const [_line, sign, ko, en, r, g] = exec;
                        items.push({
                            line: _line,
                            sign: COFFEA.sign(sign),
                            ko: ko,
                            en: en,
                            r: r === '0' ? '' : r,
                            g: g === '0' ? '' : g
                        });
                    }
                });
                return {
                    type: 'menu',
                    name: {ko: ko, en: en},
                    items: items
                }
            },
            text(lines) {
                return {type: 'text', text: lines.trim()};
            }

        },
        parse(text) {
            const result = [];
            UTIL.$prefixSplit(text).forEach(({type, text}) =>{
                if (COFFEA.types[type]) {
                    result.push(COFFEA.types[type](text));
                }
            });
            return result;
        }
    }