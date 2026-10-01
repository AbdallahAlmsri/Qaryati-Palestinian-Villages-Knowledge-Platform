INSERT INTO governorates (name) VALUES ('Nablus');

INSERT INTO villages (governorate_id, latitude, longitude, location_description, elevation_m)
VALUES (
           (SELECT id FROM governorates WHERE name = 'Nablus'),
           32.221481, 35.255573,
           'A village in Nablus Governorate', 550
       );

INSERT INTO village_names (village_id, name, name_type, language)
VALUES (
           (SELECT id FROM villages LIMIT 1),
    'Sample Village', 'current', 'en'
    );