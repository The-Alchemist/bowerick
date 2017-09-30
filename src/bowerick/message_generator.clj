;;;
;;;   Copyright 2017, Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Functions for generating messages"} 
  bowerick.message-generator
  (:require
    [clojure.java.io :as java-io]
    [clojure.string :as str]
    [clj-assorted-utils.util :as utils])
  (:import
    (java.io File FileInputStream)
    (java.nio ByteOrder MappedByteBuffer)
    (java.nio.channels FileChannel FileChannel$MapMode)))

(defn create-message-generator
  [producer delay-fn generator-name generator-args-string]
  (let [generator-construction-fn (ns-resolve
                                    'bowerick.message-generator
                                    (symbol (str generator-name "-generator")))
        tmp-args (binding [*read-eval* false]
                   (try
                     (read-string generator-args-string)
                     (catch Exception e
                       generator-args-string)))
        generator-args (if (vector? tmp-args)
                         tmp-args
                         (if (symbol? tmp-args)
                           [(str tmp-args)]
                           [tmp-args]))]
    (if (not (nil? generator-construction-fn))
      (apply generator-construction-fn  producer delay-fn generator-args))))

(defn txt-file-generator
  [producer delay-fn in-path split-regex]
  (let [lines (str/split (slurp in-path) split-regex)]
    (fn []
      (doseq [l lines]
        (producer l)
        (delay-fn)))))

(defn txt-file-line-generator
  [producer delay-fn in-path]
  (fn []
    (with-open [rdr (java-io/reader in-path)]
      (doseq [l (line-seq rdr)]
        (producer l)
        (delay-fn)))))

(defn binary-file-generator
  [producer delay-fn in-path initial-offset length-field-offset length-field-size header-size]
  (let [^File in-file (File. in-path)
        file-length (.length in-file)
        ^MappedByteBuffer buffer (with-open [^FileInputStream in-stream (FileInputStream. in-file)]
                                   (let [^FileChannel file-channel (.getChannel in-stream)
                                         ^MappedByteBuffer mbb (.map file-channel FileChannel$MapMode/READ_ONLY 0 file-length)]
                                     (.order mbb ByteOrder/LITTLE_ENDIAN)
                                     mbb))]
    (fn []
      (.rewind buffer)
      (loop [offset initial-offset]
        (let [data-size (.getInt buffer (+ offset length-field-offset))
              total-size (+ data-size header-size)
              ba (byte-array total-size)]
          (.position buffer offset)
          (.get buffer ba 0 total-size)
          (producer ba)
          (delay-fn)
          (if (< (+ offset total-size) file-length)
            (recur (+ offset total-size))))))))

(defn pcap-file-generator
  [producer delay-fn in-path]
  (binary-file-generator producer delay-fn in-path 24 8 4 16))

