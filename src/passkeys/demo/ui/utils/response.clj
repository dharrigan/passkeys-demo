(ns passkeys.demo.ui.utils.response
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.system.constants :as constants]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn html
  [body]
  (-> (response/response body)
      (response/header constants/content-type constants/html)))
