# Google ML Kit: локальный перевод

При выборе «Перевести с Google» используется [Google ML Kit on-device translation](https://developers.google.com/ml-kit/language/translation). После загрузки модели перевод выполняется на устройстве; исходный язык для ресурсов по умолчанию выбирается явно пользователем. Загрузка языковой модели требует сети и согласия пользователя. Машинный перевод предоставляется как есть, может содержать ошибки и не заменяет перевод человеком.

При отображении результатов используется атрибуция Google Translate. Изображение `apkEditor/src/main/res/drawable/google_translate_attribution.png` получено из [официального примера Google ML Kit](https://github.com/googlesamples/mlkit/tree/master/android/translate-showcase/app/src/main/res/drawable). Условия и требования к атрибуции: [ML Kit translation terms](https://developers.google.com/ml-kit/language/translation/translation-terms), [Google Cloud Translation attribution](https://cloud.google.com/translate/attribution).
