(ns passkeys.demo.ui.utils.thymeleaf
  {:author "David Harrigan"}
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as csk-extras]
   [passkeys.demo.core.utils.system :as system]
   [passkeys.demo.ui.utils.response :as response-utils])
  (:import
   [java.util Locale]
   [org.thymeleaf TemplateEngine]
   [org.thymeleaf.context Context IContext]))

(set! *warn-on-reflection* true)

(defn ^:private render
  [template-engine viewname environment locales data]
  (let [context (Context. (Locale/of (name (first locales))))] ;; use the first locale as the default for rendering i18n text
    (when (and data (map? data))
      (when-let [data' (csk-extras/transform-keys csk/->camelCaseString data)]
        (.setVariables context data')))
    (.setVariable context "environment" (name environment))
    (.setVariable context "locales" locales)
    (.setVariable context "version" system/version)
    (.process ^TemplateEngine template-engine ^String viewname ^IContext context)))

(defn html
  ([viewname request] (html viewname request nil))
  ([viewname request data]
   (let [{{{:keys [html-template-engine]} :components} :system :keys [locales]} request
         {{{:keys [environment]} :runtime-config} :system} request]
     (-> (render html-template-engine viewname environment locales data)
         (response-utils/html)))))
