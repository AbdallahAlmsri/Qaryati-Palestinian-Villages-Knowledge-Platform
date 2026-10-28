# Qaryati Database ERD

```mermaid
erDiagram

    governorates ||--o{ villages : contains

    villages ||--o{ village_names : has

    villages ||--o{ population_records : has

    villages ||--o{ historical_events : has

    villages ||--o{ landmarks : has

    villages ||--o{ village_attributes : has

    users o|--o{ village_names : creates

    users o|--o{ village_names : reviews

    users ||--o{ population_records : creates

    users o|--o{ population_records : reviews

    users o|--o{ historical_events : creates

    users o|--o{ historical_events : reviews

    users o|--o{ landmarks : creates

    users o|--o{ landmarks : reviews

    users o|--o{ village_attributes : creates

    users o|--o{ village_attributes : reviews

    governorates {

        bigint id PK

        varchar name UK

        timestamptz created_at

    }

    villages {

        bigint id PK

        bigint governorate_id FK

        decimal latitude

        decimal longitude

        text location_description

        integer elevation_m

        decimal land_area_km2

        timestamptz created_at

        timestamptz updated_at

    }

    village_names {

        bigint id PK

        bigint village_id FK

        varchar name

        varchar name_type

        varchar language

        integer valid_from

        integer valid_to

        text source

        text name_origin

        varchar status

        bigint created_by FK

        bigint reviewed_by FK

        timestamptz reviewed_at

        timestamptz created_at

    }

    users {

        bigint id PK

        varchar username UK

        varchar email UK

        varchar password_hash

        varchar role

        timestamptz created_at

    }

    population_records {

        bigint id PK

        bigint village_id FK

        integer year

        integer population

        varchar source

        varchar source_url

        text notes

        varchar status

        bigint created_by FK

        timestamp created_at

        bigint reviewed_by FK

        timestamp reviewed_at

    }

    historical_events {

        bigint id PK

        bigint village_id FK

        varchar title

        text description

        date event_date

        text source

        varchar status

        bigint created_by FK

        timestamptz created_at

        bigint reviewed_by FK

        timestamptz reviewed_at

    }

    landmarks {

        bigint id PK

        bigint village_id FK

        varchar name

        text description

        varchar landmark_type

        decimal latitude

        decimal longitude

        text source

        varchar status

        bigint created_by FK

        timestamptz created_at

        bigint reviewed_by FK

        timestamptz reviewed_at

    }

    village_attributes {

        bigint id PK

        bigint village_id FK

        varchar attribute_name

        text attribute_value

        text source

        varchar status

        bigint created_by FK

        timestamptz created_at

        bigint reviewed_by FK

        timestamptz reviewed_at

    }

```
## Notes

- Based on Flyway migrations V1 through V6.

- PK means Primary Key, FK means Foreign Key, UK means Unique Key.

- Village names, historical events, landmarks, and village attributes are deleted automatically when their village is deleted.

- Population records reference villages without an ON DELETE CASCADE rule.

- The population_records table has a unique constraint on village_id, year, and source.

- created_by and reviewed_by reference users.id.

- Optional user relationships reflect nullable foreign keys.
