# bowerick

Easing Simple Message-oriented Middleware Tasks with Clojure (and Java)

[![Clojars Project](https://img.shields.io/clojars/v/bowerick.svg)](https://clojars.org/bowerick)
[![Build Status TravisCI](https://travis-ci.org/ruedigergad/bowerick.svg?branch=master)](https://travis-ci.org/ruedigergad/bowerick)
[![Build Status SemaphoreCI](https://semaphoreci.com/api/v1/ruedigergad/bowerick/branches/master/badge.svg)](https://semaphoreci.com/ruedigergad/bowerick)
[![lein test](https://github.com/ruedigergad/bowerick/actions/workflows/lein_test.yml/badge.svg)](https://github.com/ruedigergad/bowerick/actions/workflows/lein_test.yml)
[![Coverage Status](https://coveralls.io/repos/github/ruedigergad/bowerick/badge.svg?branch=master)](https://coveralls.io/github/ruedigergad/bowerick?branch=master)

## Links

### Detailed Test Results

Detailed test results are available as well:
[https://ruedigergad.github.io/bowerick/test-results/html/](http://ruedigergad.github.io/bowerick/test-results/html/)

### Blog Posts

On my website, I wrote some blog posts about bowerick:

[https://ruedigergad.com/category/libs/bowerick/](https://ruedigergad.com/category/libs/bowerick/)

In these blog posts, I make announcements about bowerick and discuss selected aspects of bowerick in more detail.

### API Docs

API docs are available:
[https://ruedigergad.github.io/bowerick/doc/](http://ruedigergad.github.io/bowerick/doc/)

## Documentation

### Examples

#### Minimal Working Example

    ; Can also be run in: lein repl
    (require '[bowerick.jms :as jms])
    (def url "tcp://127.0.0.1:61616")
    (def destination "/topic/my.test.topic")
    (def brkr (jms/start-broker url))
    (def consumer (jms/create-json-consumer url destination (fn [data] (println "Received:" data))))
    (def producer (jms/create-json-producer url destination))
    (producer "foo")
    ; nilReceived: foo
    (producer '(1 7 0 1))
    ; nilReceived: (1 7 0 1)
    (jms/close producer)
    (jms/close consumer)
    (jms/stop brkr)
    ; (quit)

#### Multiple Transports

    ; Can also be run in: lein repl
    (require '[bowerick.jms :as jms])
    (def urls ["tcp://127.0.0.1:61616" "stomp://127.0.0.1:61617" "ws://127.0.0.1:61618" "mqtt://127.0.0.1:61619"])
    (def destination "/topic/my.test.topic")
    (def brkr (jms/start-broker urls))
    (def consumers (doall (map-indexed (fn [idx url] (jms/create-json-consumer url destination (fn [data] (Thread/sleep (* 100 idx)) (println "Received" url data)))) urls)))
    (def producer (jms/create-json-producer (first urls) destination))
    (producer "foo")
    ; Received tcp://127.0.0.1:61616 foo
    ; Received stomp://127.0.0.1:61617 foo
    ; Received ws://127.0.0.1:61618 foo
    ; Received mqtt://127.0.0.1:61619 foo
    (jms/close producer)
    (doseq [consumer consumers] (jms/close consumer))
    (jms/stop brkr)
    ; (quit)

#### Pooled Operation

    ; Can also be run in: lein repl
    (require '[bowerick.jms :as jms])
    (def url "tcp://127.0.0.1:61616")
    (def destination "/topic/my.test.topic")
    (def brkr (jms/start-broker url))
    (def pool-size 3)
    (def consumer (jms/create-json-consumer url destination (fn [data] (println "Received:" data)) pool-size))
    (def producer (jms/create-json-producer url destination pool-size))
    (producer "foo")
    (producer [1 7 0 1])
    (producer 42.0)
    ; nilReceived: foo
    ; Received: [1 7 0 1]
    ; Received: 42.0
    (jms/close producer)
    (jms/close consumer)
    (jms/stop brkr)
    ; (quit)

### API Docs

API docs are available:
http://ruedigergad.github.io/bowerick/doc/

### Cheat Sheet

#### Transport Connections

* Encrypted
    * OpenWire via TCP (with optional client authentication)

      ssl://127.0.0.1:42425
      
      ssl://127.0.0.1:42425?needClientAuth=true
    * STOMP via TCP (with optional client authentication)
      
      stomp+ssl://127.0.0.1:42423
      
      stomp+ssl://127.0.0.1:42423?needClientAuth=true
    * STOMP via WebSockets (with optional client authentication)
      
      wss://127.0.0.1:42427
      
      wss://127.0.0.1:42427?needClientAuth=true
    * MQTT via TCP (with optional client authentication)

      mqtt+ssl://127.0.0.1:42429

      mqtt+ssl://127.0.0.1:42429?needClientAuth=true
* Unencrypted
    * OpenWire via TCP
      
      tcp://127.0.0.1:42424
    * OpenWire via UDP
      
      udp://127.0.0.1:42426
    * STOMP via TCP
      
      stomp://127.0.0.1:42422

    * STOMP via WebSocket

      ws://127.0.0.1:42428
    * MQTT via TCP

      mqtt://127.0.0.1:42430

#### Serialization, Compression, and Pooling

Placeholders Used in the Cheat Sheet Examples

    (def server-url "tcp://127.0.0.1:42424")
    (def destination-description "/topic/my.topic.name")
    (def pool-size 10)
    (defn callback-fn [data] (println data))

* Default Serialization
 
 (create-producer server-url destination-description pool-size)
 
 (create-consumer server-url destination-description callback-fn pool-size)
* JSON Serialization (Using Cheshire)

 (create-json-producer server-url destination-description pool-size)
 
 (create-json-consumer server-url destination-description callback-fn pool-size)
* Carbonite (Kryo) Serialization
 
 (create-carbonite-producer server-url destination-description pool-size)
 
 (create-carbonite-consumer server-url destination-description callback-fn pool-size)
* Carbonite (Kryo) Serialization with LZF Compression
 
 (create-carbonite-lzf-producer server-url destination-description pool-size)
 
 (create-carbonite-lzf-consumer server-url destination-description callback-fn pool-size)
* Nippy Serialization
 
 (create-nippy-producer server-url destination-description pool-size)
 
 (create-nippy-consumer server-url destination-description callback-fn pool-size)
* Nippy Serialization with Compression (LZ4, Snappy, LZMA2)
 
 (create-nippy-producer server-url destination-description pool-size {:compressor taoensso.nippy/lz4-compressor})
 
 (create-nippy-producer server-url destination-description pool-size {:compressor taoensso.nippy/snappy-compressor})
 
 (create-nippy-producer server-url destination-description pool-size {:compressor taoensso.nippy/lzma2-compressor})
* Nippy Serialization with LZF Compression
 
 (create-nippy-lzf-producer server-url destination-description pool-size)
 
 (create-nippy-lzf-consumer server-url destination-description callback-fn pool-size)

## License

Copyright © 2016 - 2018 Ruediger Gad

Copyright © 2014 - 2015 Frankfurt University of Applied Sciences

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

