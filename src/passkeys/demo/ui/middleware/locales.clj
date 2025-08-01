(ns passkeys.demo.ui.middleware.locales
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log])
  (:import
   [java.util Locale Locale$LanguageRange]))

(set! *warn-on-reflection* true)

(defn ^:private parse-accept-language
  [header]
  (mapv (fn [language-range]
          (-> ^String (.getRange ^Locale$LanguageRange language-range)
              ^Locale (Locale/forLanguageTag)
              (str)
              (keyword))) (Locale$LanguageRange/parse header)))

(defn ^:private with-locales
  [handler]
  (fn [request]
    (let [header (get-in request [:headers "accept-language"] "en-GB")
          locales (parse-accept-language header)]
      (log/tracef "Parsed accept-language '%s' to '%s'" header locales)
      (handler (assoc request :locales locales)))))

(def locales-middleware
  {:name ::locales
   :wrap with-locales})
