# Что исправлено: скачивание файлов и установка расширений

## Скачивание файлов
- `BrowserScreen.kt`: DownloadListener отправлял в «установку расширения» всё с типом `octet-stream` / `zip`,
  а все остальные файлы (pdf, apk, видео, картинки…) не скачивались вообще. Теперь:
  - расширения (`.crx`, `.kubik`, `.xpi`, папки Kubik Store) → установка;
  - всё остальное → системный DownloadManager (cookie, User-Agent и Referer страницы), файл в «Загрузки»;
  - `blob:` и `data:` ссылки сохраняются через MediaStore (на Android 9 и старше — с запросом разрешения).
- `.zip` больше не считается расширением.
- Новый `util/FileDownloader.kt`.
- Манифест: `WRITE_EXTERNAL_STORAGE` только для Android 9 и ниже (`maxSdkVersion=28`).

## Установка расширений
- Ошибка разбора при установке из файла раньше молчала — теперь показывается сообщение.
- Тосты вызывались из потока JavaBridge (кнопка «Установить в Kopi» в Kubik Store) → вылет. Теперь через главный поток.
- Новый `ExtensionDownloader.kt`: User-Agent, cookie, проверка кода ответа, редиректы (в т.ч. http→https), имя из Content-Disposition.
- `CrxParser.kt`: заголовки CRX2/CRX3, комментарии и BOM в manifest.json, названия `__MSG_имя__` из `_locales`,
  CSS из content_scripts, пропуск ES-модулей (service worker с `import`), каждый файл в своём try/catch.
- `ExtensionEngine.kt`: шаблоны `*://*.site.com/*` раньше не срабатывали; каждый скрипт теперь выполняется отдельным
  `evaluateJavascript` (одна ошибка больше не ломает polyfill, остальные расширения и кнопки магазина).
- Тесты в `BrowserUnitTest.kt`: определение расширений, CRX3 + локализация.

## Безопасность
- `saveBase64` в мосте принимает данные только с одноразовым токеном от самого браузера.
