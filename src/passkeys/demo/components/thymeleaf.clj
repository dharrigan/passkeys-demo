(ns passkeys.demo.components.thymeleaf
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.utils.i18n :refer [resolve-message]])
  (:import
   [org.thymeleaf TemplateEngine]
   [org.thymeleaf.messageresolver IMessageResolver]
   [org.thymeleaf.templateresolver ClassLoaderTemplateResolver]))

(set! *warn-on-reflection* true)

(def ^:private template-resolver-defaults {:prefix "templates/html" :suffix ".html" :cacheable false :cache-ttl-ms 1000})

(def error-template-aliases
  {"errorSection" "exceptions/error-section"})

(def ^:private login-template-aliases
  {"loginFooter" "login/common/components/footer"
   "loginHeader" "login/common/components/header"})

(def ^:private dashboard-template-aliases
  {"dashboardBase" "dashboard/base"
   "dashboardFooter" "dashboard/common/components/footer"
   "dashboardHeader" "dashboard/common/components/header"
   "dashboardNavbar" "dashboard/common/components/navbar"})

(defn ^:private classpath-template-resolver
  [config]
  (let [{:keys [prefix suffix cacheable cache-ttl-ms]} (merge template-resolver-defaults config)
        class-loader-template-resolver (doto (ClassLoaderTemplateResolver.)
                                         (.setCacheable cacheable)
                                         (.setCacheTTLMs cache-ttl-ms)
                                         (.setPrefix (str prefix "/"))
                                         (.setSuffix suffix))]
    (.setTemplateAliases class-loader-template-resolver (merge error-template-aliases login-template-aliases dashboard-template-aliases))
    class-loader-template-resolver))

(def ^:private tempura-message-resolver
  (reify IMessageResolver
    (getName [_] "Tempura Message Resolver")
    (getOrder [_] (int 0))
    (resolveMessage [_ context _ key message-parameters]
      (resolve-message (.getVariable context "locales") (keyword key) message-parameters))
    (createAbsentMessageRepresentation [_ _ _ _ _]
      (comment "This will never fire as the `resolve-message` above *always* returns something, even indicating a missing key"))))

;; DONUT LIFECYCLE FUNCTIONS ↓

(defn start
  ^ClassLoaderTemplateResolver
  [config]
  (doto (TemplateEngine.)
    (.setTemplateResolver (classpath-template-resolver config))
    (.addMessageResolver tempura-message-resolver)))
