ALTER TABLE `country`
    ADD COLUMN `chinese_name` VARCHAR(100) NULL;

UPDATE `country`
SET `chinese_name` = CASE UPPER(`code`)
    WHEN 'AE' THEN '阿联酋'
    WHEN 'AR' THEN '阿根廷'
    WHEN 'AT' THEN '奥地利'
    WHEN 'AU' THEN '澳大利亚'
    WHEN 'BE' THEN '比利时'
    WHEN 'BR' THEN '巴西'
    WHEN 'CA' THEN '加拿大'
    WHEN 'CH' THEN '瑞士'
    WHEN 'CN' THEN '中国大陆'
    WHEN 'CZ' THEN '捷克'
    WHEN 'DE' THEN '德国'
    WHEN 'DK' THEN '丹麦'
    WHEN 'ES' THEN '西班牙'
    WHEN 'FI' THEN '芬兰'
    WHEN 'FR' THEN '法国'
    WHEN 'GB' THEN '英国'
    WHEN 'GR' THEN '希腊'
    WHEN 'HK' THEN '香港'
    WHEN 'ID' THEN '印度尼西亚'
    WHEN 'IE' THEN '爱尔兰'
    WHEN 'IL' THEN '以色列'
    WHEN 'IN' THEN '印度'
    WHEN 'IR' THEN '伊朗'
    WHEN 'IT' THEN '意大利'
    WHEN 'JP' THEN '日本'
    WHEN 'KR' THEN '韩国'
    WHEN 'MX' THEN '墨西哥'
    WHEN 'MY' THEN '马来西亚'
    WHEN 'NL' THEN '荷兰'
    WHEN 'NO' THEN '挪威'
    WHEN 'NZ' THEN '新西兰'
    WHEN 'PH' THEN '菲律宾'
    WHEN 'PL' THEN '波兰'
    WHEN 'PT' THEN '葡萄牙'
    WHEN 'RU' THEN '俄罗斯'
    WHEN 'SE' THEN '瑞典'
    WHEN 'SG' THEN '新加坡'
    WHEN 'TH' THEN '泰国'
    WHEN 'TR' THEN '土耳其'
    WHEN 'TW' THEN '台湾'
    WHEN 'US' THEN '美国'
    WHEN 'VN' THEN '越南'
    WHEN 'ZA' THEN '南非'
    ELSE `chinese_name`
END
WHERE UPPER(`code`) IN (
    'AE', 'AR', 'AT', 'AU', 'BE', 'BR', 'CA', 'CH', 'CN', 'CZ',
    'DE', 'DK', 'ES', 'FI', 'FR', 'GB', 'GR', 'HK', 'ID', 'IE',
    'IL', 'IN', 'IR', 'IT', 'JP', 'KR', 'MX', 'MY', 'NL', 'NO',
    'NZ', 'PH', 'PL', 'PT', 'RU', 'SE', 'SG', 'TH', 'TR', 'TW',
    'US', 'VN', 'ZA'
);
