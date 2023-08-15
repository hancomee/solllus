const
    __ = (html, obj) => JS.replace(/\{([^}]+)\}/g, (g, p) => obj[p]),
    $container = document.getElementById('box'),
    menu = [
        {
            category: '메뉴',
            list: '주먹고기_14,000  통삽겹살_14,000  등심덧살_15,000  천겹살_16,000  토시살_15,000  껍데기_9,000'.split('  ')
        },
        {
            category: '식사류',
            list: '된장술밥_5,000  김치국밥_5,000  냉열무 김치말이국수_5,000'.split('  '),
        },
        {
            category: '기타류',
            list: '공기밥_1,000  새송이버섯_1,000  명이나물_2,000'.split('  '),
        },
        {
            category: '주류 및 음료',
            list: '일품진로_24,000  일반소주_5,000  지역소주_6,000  맥주_5,000  클라우드_6,000  음료수_2,000'.split('  '),
        },
    ],
    __html = (list) => {
        return '<div class="menu-container"><div class="menu">' + list.map(obj => {
            let
                {category, list} = obj,
                array = ['<div class="header">' + category + '</div>'];
            array[1] = list.map(value => {
                return __('<div class="body"><span class="name">{0}</span><b></b><span class="money">{1}</span></div>', value.split('_'));
            }).join('');
            return array.join('');
        }).join('') + '</div></div>'
    };

$container.innerHTML = [[menu[0]], [menu[1], menu[2], menu[3]]].map( list => __html(list)).join('');

console.log('랍똥');