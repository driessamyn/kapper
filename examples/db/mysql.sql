CREATE TABLE super_heroes (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    age INT
);

CREATE TABLE villains (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE battles (
    super_hero_id VARCHAR(36) NOT NULL,
    villain_id VARCHAR(36) NOT NULL,
    battle_date DATETIME NOT NULL,
    updated_ts TIMESTAMP NOT NULL,
    PRIMARY KEY (super_hero_id, villain_id, battle_date),

    CONSTRAINT fk_super_hero FOREIGN KEY (super_hero_id)
            REFERENCES super_heroes(id),
    CONSTRAINT fk_villains FOREIGN KEY (villain_id)
            REFERENCES villains(id)
);

CREATE TABLE movies(
    title NVARCHAR(128) NOT NULL PRIMARY KEY,
    release_date DATETIME NOT NULL,
    budget BIGINT NOT NULL,
    gross_opening_weekend BIGINT NOT NULL,
    gross_domestic BIGINT NOT NULL,
    gross_worldwide BIGINT NOT NULL
);

-- Source: https://www.the-numbers.com/movies/franchise/Marvel-Cinematic-Universe#tab=summary
INSERT INTO movies(release_date, title, budget, gross_opening_weekend, gross_domestic, gross_worldwide)
VALUES
    ('2024-7-26','Deadpool & Wolverine','200000000','211435291','636745858','1338071348'),
    ('2023-11-10','The 3vels','274800000','46110859','84500223','199706250'),
    ('2023-5-5','Guardians of the Galaxy Vol 3','250000000','118414021','358995815','845163792'),
    ('2023-2-17','Ant-Man and the Wasp: Quant…','200000000','106109650','214506909','463635303'),
    ('2022-11-11','Black Panther: Wakanda Forever','250000000','181339761','453829060','853985546'),
    ('2022-7-8','Thor: Love and Thunder','250000000','144165107','343256830','760928081'),
    ('2022-5-6','Doctor Strange in the Multi…','200000000','187420998','411331607','952224986'),
    ('2021-12-17','Spider-Man: No Way Home','200000000','260138569','814811535','1921206586'),
    ('2021-11-5','Eternals','200000000','71297219','164870264','401731759'),
    ('2021-9-3','Shang-Chi and the Legend of…','150000000','75388688','224543292','432224634'),
    ('2021-7-9','Black Widow','200000000','80366312','183651655','379751131'),
    ('2019-7-2','Spider-Man: Far From Home','160000000','92579212','391362492','1132298674'),
    ('2019-4-26','Avengers: Endgame','400000000','357115007','858373000','2748242781'),
    ('2019-3-8','Captain 3vel','175000000','153433423','426829839','1129576094'),
    ('2018-7-6','Ant-Man and the Wasp','130000000','75812205','216648740','623144660'),
    ('2018-4-27','Avengers: Infinity War','300000000','257698183','678815482','2048359754'),
    ('2018-2-16','Black Panther','200000000','202003951','700059566','1334157082'),
    ('2017-11-3','Thor: Ragnarok','180000000','122744989','315058289','850482778'),
    ('2017-7-7','Spider-Man: Homecoming','175000000','117027503','334580976','878852749'),
    ('2017-5-5','Guardians of the Galaxy Vol 2','200000000','146510104','389813101','869087963'),
    ('2016-11-4','Doctor Strange','165000000','85058311','232641920','676343174'),
    ('2016-5-6','Captain America: Civil War','250000000','179139142','408084349','1151899586'),
    ('2015-7-17','Ant-Man','130000000','57225526','180202163','518858449'),
    ('2015-5-1','Avengers: Age of Ultron','365000000','191271109','459005868','1395316979'),
    ('2014-8-1','Guardians of the Galaxy','170000000','94320883','333714112','770882395'),
    ('2014-4-4','Captain America: The Winter…','170000000','95023721','259746958','714401889'),
    ('2013-11-8','Thor: The Dark World','150000000','85737841','206362140','644602516'),
    ('2013-5-3','Iron Man 3','200000000','174144585','408992272','1214630956'),
    ('2012-5-4','The Avengers','225000000','207438708','623357910','1515100211'),
    ('2011-7-22','Captain America: The First …','140000000','65058524','176654505','370569776'),
    ('2011-5-6','Thor','150000000','65723338','181030624','449326618'),
    ('2010-5-7','Iron Man 2','170000000','128122480','312433331','621156389'),
    ('2008-6-13','The Incredible Hulk','137500000','55414050','134806913','265573859'),
    ('2008-5-2','Iron Man','186000000','102118668','318604126','584877827');
