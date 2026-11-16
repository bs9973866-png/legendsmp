# LegendSMP

[![Build](https://img.shields.io/github/actions/workflow/status/ТВОЙ_НИК/LegendSMP/build.yml?branch=main&label=build&logo=githubactions&logoColor=white)](https://github.com/ТВОЙ_НИК/LegendSMP/actions)
[![Downloads](https://img.shields.io/github/downloads/ТВОЙ_НИК/LegendSMP/total?label=скачиваний&logo=github)](https://github.com/ТВОЙ_НИК/LegendSMP/releases)
[![Views](https://hits.sh/github.com/ТВОЙ_НИК/LegendSMP.svg?label=просмотров&color=blue)](https://hits.sh/github.com/ТВОЙ_НИК/LegendSMP/)
[![Stars](https://img.shields.io/github/stars/ТВОЙ_НИК/LegendSMP?style=flat&label=звёзд&logo=github)](https://github.com/ТВОЙ_НИК/LegendSMP/stargazers)
[![Release](https://img.shields.io/github/v/release/ТВОЙ_НИК/LegendSMP?label=версия)](https://github.com/ТВОЙ_НИК/LegendSMP/releases/latest)
![Paper](https://img.shields.io/badge/Paper-1.21.11-blue)
![Java](https://img.shields.io/badge/Java-21-orange)

Плагин для **Paper 1.21.11** (Java 21): легендарное оружие с прокачкой от убийств,
броня со сет-бонусом и крафты, для которых нужны головы игроков.

## Возможности

- **Оружие с прокачкой:** Меч Теней, Топор Берсерка, Лук Охотника.
  Каждые 10 убийств = +1 уровень (макс. 5), +8% урона за уровень.
- **Броня Стража (4 части):** −6% входящего урона за часть, +6% за полный сет (итого −30%).
- **Крафты с головами игроков.** Голова падает только если игрока убил другой игрок.
- **Рецепты для всех:** открываются в книге рецептов при входе; новичок получает «Книгу Легенд»
  (ПКМ — меню рецептов). Меню также открывает `/legend`.

## Рецепты

| Предмет | Ингредиенты |
|---|---|
| Меч Теней | 2 головы, 1 алмаз, 1 незеритовый слиток, 1 палка |
| Топор Берсерка | 1 голова, 2 алмаза, 1 незеритовый слиток, 2 палки |
| Лук Охотника | 1 голова, 2 незеритовых слитка, 3 нити |
| Шлем Стража | 1 голова, 4 алмаза |
| Нагрудник Стража | 1 голова, 7 алмазов |
| Поножи Стража | 1 голова, 6 алмазов |
| Ботинки Стража | 2 головы, 2 алмаза |

## Команды

- `/legend` — меню рецептов
- `/legend give <id>` — выдать предмет (только оператор).
  id: `shadow_sword`, `berserk_axe`, `hunter_bow`, `guardian_helmet`, `guardian_chest`, `guardian_legs`, `guardian_boots`

## Установка

1. Скачай `LegendSMP.jar` из раздела **Releases** (или из **Actions → Artifacts**).
2. Положи в папку `plugins/` сервера Paper 1.21.11.
3. Перезапусти сервер.

## Сборка

Нужны JDK 21 и Maven:

```bash
mvn package
```

Готовый файл: `target/LegendSMP.jar`.

## Автосборка на GitHub

Workflow `.github/workflows/build.yml` собирает плагин при каждом push и pull request,
а jar доступен в **Actions → Artifacts**. Чтобы выпустить релиз:

```bash
git tag v1.0
git push origin v1.0
```

Jar автоматически прикрепится к новому релизу.

## Настройка

Числа (убийств на уровень, бонусы урона и брони) находятся в начале
`src/main/java/ru/legend/LegendSMP.java`.
