<h1 align="center">Search Engine</h1>

----
<h2 align="center"> 
<p>Java-разработчик с нуля.</p>
<p>Итоговый проект курса «Поисковый движок»</p>
</h2>

----
<p align="center">
<img src="imagesForReadme/1.gif"></p>
<h2 align="center"><a  href="https://www.youtube.com/watch?v=VEODIthri6A">Демонстрация работы приложения</a></h2>

----
## Stack
Java, Spring Boot, Maven, JDBC, Hibernate, SQL, JSOUP, Morphology Library, Lombok.
____
## Краткое описание
Данное Spring Boot приложение является локальным поисковым движком,
основной функциональностью которого являются:
<li>отображение общей и детальной статистики по сайтам, указанных в конфигурационном файле - application.yaml;</li>
<li>индексация страниц сайтов;</li>
<li>поиск по данным страницам.</li>

____
## Вкладка DASHBOARD
<p align="center">
<img src="imagesForReadme/2.png"></p>

Вкладка по умолчанию. На ней отображаются - 
### Общая статистика:
Количество сайтов, проиндексированных страниц и лем (нормальных форм слов);
### Детальная статистика:
Время индексации,
количество проиндексированных страниц и лем конкретного сайта,   
а также сообщение об ошибке, если таковая произошла.
### API request - GET /api/statistics
<details>
<summary>JSON Response</summary> 

{\
&emsp;'result': true,\
&emsp;'statistics': {\
&emsp;&emsp;"total": {\
&emsp;&emsp;&emsp;"sites": 10,\
&emsp;&emsp;&emsp;"pages": 436423,\
&emsp;&emsp;&emsp;"lemmas": 5127891,\
&emsp;&emsp;&emsp;"indexing": true\
&emsp;&emsp;},\
&emsp;&emsp;"detailed": [\
&emsp;&emsp;&emsp;{\
&emsp;&emsp;&emsp;&emsp;"url": "https://dombulgakova.ru",\
&emsp;&emsp;&emsp;&emsp;"name": "DomBulgakova",\
&emsp;&emsp;&emsp;&emsp;"status": "INDEXED",\
&emsp;&emsp;&emsp;&emsp;"statusTime": 1600160357,\
&emsp;&emsp;&emsp;&emsp;"error": "Ошибка индексации: главная страница сайта недоступна",\
&emsp;&emsp;&emsp;&emsp;"pages": 5764,\
&emsp;&emsp;&emsp;&emsp;"lemmas": 321115\
&emsp;&emsp;&emsp;&emsp;},\
&emsp;&emsp;&emsp;...\
&emsp;&emsp;]\
}
</details> 

___
## Вкладка MANAGEMENT
<p align="center">
<img src="imagesForReadme/3.png"></p>
<p align="center">
<img src="imagesForReadme/4.png"></p>

Вкладка управления индексацией.
### Кнопка START INDEXING:
Запуск полной индексации;
### API request - GET /api/startIndexing
<details>
<summary>JSON Response</summary> 
 <h3>OK</h3>

{\
&emsp;'result': true\
}
______
<h3>ERROR</h3>

{\
&emsp;'result': false,\
&emsp;'error': "Индексация уже запущена"\
}
</details> 

___
### Кнопка STOP INDEXING:
Остановка полной индексации;
### API request - GET /api/stopIndexing
<details>
<summary>JSON Response</summary> 
 <h3>OK</h3>

{\
&emsp;'result': true\
}
______
<h3>ERROR</h3>

{\
&emsp;'result': false,\
&emsp;'error': "Индексация не запущена"\
}
</details> 

___
### Кнопка ADD/UPDATE и строка ввода:
Запуск индексации страницы введенного URL.
### API request - POST /api/indexPage?{url}
Параметры:
* url — адрес страницы, которую нужно проиндексировать.
<details>
<summary>JSON Response</summary> 
 <h3>OK</h3>

{\
&emsp;'result': true\
}
______
<h3>ERROR</h3>

{\
&emsp;'result': false,\
&emsp;'error': "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"\
}
</details> 

___
## Вкладка SEARCH
<p align="center">
<img src="imagesForReadme/5.png"></p>

Вкладка поиска.
### Выпадающий список:
По умолчанию - All sites, можно выбрать конкретный сайт и осуществить поиск по нему.
### Кнопка SEARCH и строка ввода:
Запуск поиска по введенной в строку информации.
Результаты поиска отображаются на этой же странице ниже.
### API request - GET /api/search?{query}&{site}&{limit}&{offset}  
Параметры:
* query — поисковый запрос;
* site — URL сайта, по которому осуществлять поиск (если не задан, поиск
  должен происходить по всем проиндексированным сайтам);
* offset — сдвиг от 0 для постраничного вывода (параметр
  необязательный; если не установлен, то значение по умолчанию равно
  нулю);
* limit — количество результатов, которое необходимо вывести (параметр
  необязательный; если не установлен, то значение по умолчанию равно
  20).
<details>
<summary>JSON Response</summary> 
 <h3>OK</h3>

{\
&emsp;'result': true,\
&emsp;'count': 574,\
&emsp;'data': [\
&emsp;&emsp;&emsp;{\
&emsp;&emsp;&emsp;&emsp;"site": "https://dombulgakova.ru",\
&emsp;&emsp;&emsp;&emsp;"siteName": "DomBulgakova",\
&emsp;&emsp;&emsp;&emsp;"uri": "/biblioteka-bulgakovskogo-doma",\
&emsp;&emsp;&emsp;&emsp;"title": "Библиотека Булгаковского Дома - Булгаковский Дом",\
&emsp;&emsp;&emsp;&emsp;"snippet": "...ться с современными \<b>авторами\</b>. В «Библиотеке» вы ...Мы сообщаем о новых \<b>авторах\</b> и...",\
&emsp;&emsp;&emsp;&emsp;"relevance": 0.93362\
&emsp;&emsp;&emsp;},\
&emsp;&emsp;&emsp;...\
&emsp;]\
}
______
<h3>ERROR</h3>

{\
&emsp;'result': false,\
&emsp;'error': "Задан пустой поисковый запрос"\
}
</details> 

___
## Как запустить
Для работы приложения необходимо:
<li>установить MySQL (8.0);</li>
<li>настроить соединение с базой данных по конфигурации - application.yaml.</li>

```
server:
  port: 8080

spring:
  datasource:
    username: root
    password: password
    url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
```
____