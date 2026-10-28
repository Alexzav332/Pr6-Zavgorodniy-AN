# Практическая работа №6 (вариант 12)

Небольшая логическая задача с ветвлениями: маршрутизация служебных заявок.
Вариант 12: часть А – маршрут `EXPRESS`, часть Б – метод `addExpressEligibility`.
Пакет `ru.edu.pr03` сохранён по требованию методички (работа нумеруется как №6).

## Сборка и запуск
```
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr03.SelfCheck
```
Windows: `run_selfcheck.bat`, Unix: `run_selfcheck.sh` (запускать из корня проекта – нужен `data/sample.csv`).

Режимы SelfCheck: без аргументов – unit, data-driven, затем интерактивный
(формат `amount;urgent;risk;personal`, выход: `exit` или пустая строка).
Отдельно: `java -cp out ru.edu.pr03.SelfCheck unit|data|interactive`.

## Диаграммы
```
cd diagrams
dot -Tpng activity.dot -o activity.png
dot -Tpng class.dot -o class.png
```
