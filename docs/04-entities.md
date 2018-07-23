# Сущности проекта

## Certificate

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| city | string | Город | - |
| number | string | Номер | - |
| dateFrom | DateFrom | Дата начала действия | - |
| dateTo | DateTo | Дата окончания действия | - |
| client | Client | Клиент | - |
| type | CertificateType | Тип справки | - |
| document | Document | Документ | - |

## CertificateType

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| downloadable | boolean | Доступен для скачивания | false |
| showPhoto | boolean | Отображать фото клиента | false |
| showDate | boolean | Отображать дату ниже ФИО сотрудника | false |
| contentHeaderLeft | text | Содержимое левого верхнего блока | - |
| contentHeaderRight | text | Содержимое правого верхнего блока | - |
| contentBodyRight | text | Содержимое среднего блока | - |
| contentFooter | text | Содержимое нижнего блока | - |

## Client


| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| photoName | string | Название файла с фотографией | - |
| birthDate | date | Дата рождения | - |
| birthPlace | string | Место рождения | - |
| gender | integer | Пол (1 - м, 2 - ж) | - |
| firstname | string | Имя | - |
| lastname | string | Фамилия | - |
| middlename | string | Отчество | - |
| lastResidenceDistrict | District | Место последнего проживания | - |
| lastRegistrationDistrict | District | Место последней регистрации | - |
| fieldValues | ClientFieldValue | Значения дополнительных полей | - |
| notes | [Note] | Примечания | - |
| contracts | [Contract] | Договоры | - |
| documents | [Document] | Документы | - |
| shelterHistories | [ShelterHistory] | Данные о проживаниях в приюте | - |
| documentFiles | [DocumentFile] | Загруженные файлы документов | - |
| services | [Service] | Полученные услуги | - |
| certificates | [Certificate] | Справки | - |
| generatedDocuments | [GeneratedDocument] | Сгенерированные документы | - |

## ClientField

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| code | string | Символьный код | - |
| enabled | boolean | Включено | true |
| type | integer | Тип | - |
| required | boolean | Обязательное поле | false |
| multiple | boolean | Допускается выбор нескольких вариантов | false |
| description | string | Подсказка | - |
| options | [ClientFieldOption] | Поле | - |

## ClientFieldOption

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| notSingle | boolean | true - больше 1-го значения | - |
| field | ClientField | Поле | - |

## ClientFieldValue

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| field | ClientField | Поле | - |
| client | Client | Клиент | - |
| text | text | Значене поля - текст | - |
| datetime | datetime | Значение поля - дата/время | - |
| option | ClientFieldOption | Вариант значения (если не multiple) | - |
| options | ClientFieldOption | Варианты значения (если не multiple) | - |
| filename | string | Имя файла для файлового поля | - |
| file | UploadedField | Значение поля - файл | - |

## Contract

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| comment | text | Комментарий | - |
| number | string | Номер | - |
| dateFrom | date | Дата начала | - |
| dateTo | date | Дата завершения | - |
| client | Client | Клиент | - |
| status | ContractStatus | Статус | - |
| document | Document | Документ | - |
| items | [ContractItem] | Пункты | - |

## ContractItem

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| comment | text | Комментарий | - |
| dateStart | date | Дата начала выполнения | - |
| date | date | Дата выполнения | - |
| contract | Contract | Договор | - |
| type | ContractItemType | Тип | - |

## ContractItemType

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| shortName | string | Сокращенное название | - |

## ContractStatus

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |

## District

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| region | Region | Регион | - |

## Document

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| address | string | Адрес | - |
| city | string | Город | - |
| date | date | Дата | - |
| number | string | Номер | - |
| numberPrefix | string | Серия | - |
| registration | integer | Регистрация | - |
| issued | string | Кем и когда выдан | - |
| client | Client | Клиент | - |
| type | DocumentType | Тип | - |

## DocumentFile

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| comment | text | Комментарий | - |
| client | Client | Клиент | - |
| type | DocumentType | Тип | - |
| filename | string | Имя файла | - |
| file | Vich\UploadableField | Файл | - |

## DocumentType

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| type | integer | Тип | - |

## GeneratedDocument

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| client | Client | Клиент | - |
| number | string | Номер | - |
| type | GeneratedDocumentType | Тип | - |
| startText | GeneratedDocumentStartText | Начальный текст | - |
| endText | GeneratedDocumentEndText | Конечный текст | - |
| text | text | Текст | - |
| whom | text | Для кого | - |
| signature | text | Подпись | - |

## GeneratedDocumentEndText

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| code | string | Код | - |
| text | text | Текст | - |


## GeneratedDocumentStartText

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| code | string | Код | - |
| text | text | Текст | - |

## GeneratedDocumentType

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| code | string | Код | - |

## History

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| client | Client | Клиент | - |

## HistoryDownload

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| client | Client | Клиент | - |
| user | User | Клиент | - |
| date | date | Дата | - |
| certificateType | CertificateType | Клиент | - |

## MenuItem

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| code | string | Код | - |
| enabled | boolean | Включено | true |

## Note 

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| text | text | Текст | - |
| client | Client | Клиент | - |
| important | boolean | Важное | true |

## Notice

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| text | text | Текст | - |
| date | date | Дата | - |
| client | Client | Клиент | - |
| viewedBy | User | Кем просмотрено | - |

## Position

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| users | [User] | Пользователи с данной должностью | - |

## Region

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| shortName | string | Сокращённое название | - |
| districts | [Dictrict] | Районы | - |

## Service

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| comment | text | Комментарий | - |
| amount | integer | Сумма денег | - |
| client | Client | Клиент | - |

## ServiceType

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |
| pay | boolean | Платная | - |
| document | Document | Документ | - |

## ShelterHistory

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| comment | text | Комментарий | - |
| diphtheriaVaccinationDate | date | Дата прививки от дифтерии | - |
| fluorographyDate | date | Дата флюорографии | - |
| hepatitisVaccinationDate | date | Дата прививки от гепатита | - |
| typhusVaccinationDate | date | Дата прививки от тифа | - |
| dateFrom | date | Дата заселения | - |
| dateTo | date | Дата выселения | - |
| room | ShelterRoom | Комната | - |
| client | Client | Клиент | - |
| status | ShelterStatus | Статус | - |
| contract | Contract | Договор | - |

## ShelterRoom

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| number | string | Номер | - |
| maxOccupants | integer | Максимальное кол-во жильцов | - |
| currentOccupants | integer | Текущее кол-во жильцов | - |
| comment | text | Комментарий | - |

## ShelterStatus

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| name | string | Название | - |

## User

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| firstname | string | Имя | - |
| lastname | string | Фамилия | - |
| middlename | string | Отчество | - |
| position | Position | Должность | - |
| proxyDate | date | Дата доверенности | - |
| proxyNum | string | Номер доверенности | - |
| passport | string | Паспортные данные | - |
| viewedNotices | [Notice] | Просмотренные уведомления | - |
| viewedClients | [ViewedClient] | Просмотренные анкеты клиентов | - |
| createdBy | date | Дата создания | - |
| updatedBy | date | Дата обновления | - |


## ViewedClient

| Свойство | Тип  | Описание  | По умолчанию  |
|---|---|---|---|
| client | Client | Клиент | - |
| createdBy | User | Кем создано | - |
