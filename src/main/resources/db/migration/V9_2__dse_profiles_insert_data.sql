INSERT INTO dse_profile(id, url, entry) VALUES (1, 'https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab', '{  "url": "https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab",  "display": "Profile - Observation - Laboruntersuchung",  "fields":[    {      "id": "Observation.value[x]",      "display": "Actual result",      "name": "value[x]",      "children": [        {          "id": "Observation.value[x]:valueQuantity",          "display": "Actual result",          "name": "valueQuantity",          "children": [            {              "id": "Observation.value[x]:valueQuantity.id",              "display": "Unique id for inter-element referencing",              "name": "id"            },            {              "id": "Observation.value[x]:valueQuantity.extension",              "display": "Additional content defined by implementations",              "name": "extension"            },            {              "id": "Observation.value[x]:valueQuantity.value",              "display": "Numerical value (with implicit precision)",              "name": "value"            },            {              "id": "Observation.value[x]:valueQuantity.comparator",              "display": "< | <= | >= | > - how to understand the value",              "name": "comparator"            },            {              "id": "Observation.value[x]:valueQuantity.unit",              "display": "Unit representation",              "name": "unit"            },            {              "id": "Observation.value[x]:valueQuantity.system",              "display": "System that defines coded unit form",              "name": "system"            },            {              "id": "Observation.value[x]:valueQuantity.code",              "display": "Coded form of the unit",              "name": "code"            }          ]        },        {          "id": "Observation.value[x]:valueCodeableConcept",          "display": "Actual result",          "name": "valueCodeableConcept",          "children": [            {              "id": "Observation.value[x]:valueCodeableConcept.id",              "display": "Unique id for inter-element referencing",              "name": "id"            },            {              "id": "Observation.value[x]:valueCodeableConcept.extension",              "display": "Additional content defined by implementations",              "name": "extension"            },            {              "id": "Observation.value[x]:valueCodeableConcept.coding",              "display": "Code defined by a terminology system",              "name": "coding",              "children": [                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.id",                  "display": "Unique id for inter-element referencing",                  "name": "id"                },                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.extension",                  "display": "Additional content defined by implementations",                  "name": "extension"                },                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.system",                  "display": "Identity of the terminology system",                  "name": "system"                },                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.version",                  "display": "Version of the system - if relevant",                  "name": "version"                },                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.code",                  "display": "Symbol in syntax defined by the system",                  "name": "code"                },                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.display",                  "display": "Representation defined by the system",                  "name": "display"                },                {                  "id": "Observation.value[x]:valueCodeableConcept.coding.userSelected",                  "display": "If this coding was chosen directly by the user",                  "name": "userSelected"                }              ]            },            {              "id": "Observation.value[x]:valueCodeableConcept.text",              "display": "Plain text representation of the concept",              "name": "text"            }          ]        }      ]    }  ],  "filters":[    {      "type": "token",      "name": "code",      "ui_type": "code",      "referencedCriteriaSet": "http://fdpg.mii.cds/CriteriaSet/Diagnose/icd-10-gm"    },    {      "type": "date",      "name": "date",      "ui_type": "timeRestriction"    }  ]}');