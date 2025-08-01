(ns passkeys.demo.core.utils.system
  {:author "David Harrigan"}
  (:require
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defonce version (if-let [version-txt (io/resource "version.txt")] (slurp version-txt) "0.0.1"))
