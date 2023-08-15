const Timer = (function () {

    const _to_num = ([a, b]) => {
        return (parseInt(a) * 60 * 60) + (parseInt(b) * 60);
    };

    return {
        parse(value) {
            const array = value.trim().split(/\n\s*\n/),
                result = [];

            array.forEach(v => {
                let [time, title, ...text] = v.split(/\n/),
                    r_time = /([!@])?(\d{2}:\d{2}).*?(\d{2}:\d{2})/.exec(time);
                if (r_time && title) {
                    const [, alert, start, end] = r_time,
                        data = {
                            count: alert == null,
                            _start: start,
                            _end: end,
                            start: _to_num(start.split(':')),
                            end: _to_num(end.split(':')),
                            title: title.trim(),
                            text: text.map(line => line.trim()).join('\n')
                        }
                    result.push(data);
                }
            });
            return result;
        }
    };
})()