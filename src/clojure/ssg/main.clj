(ns ssg.main
  (:require [clojure.java.io :refer [file]]
            [clojure.string :as s]
            [clojure.java.shell :refer [sh]]
            [markdown.core :as md]
            [hiccup2.core :as h]
            [ssg.head :refer [page-head]]
            [ssg.menu :refer [page-menu]]))

(defn md->hiccup
  "Given a Markdown string,
  return a raw HTML object for insertion in Hiccup."
  [md-string]
  (h/raw (md/md-to-html-string md-string)))

(defn post-html-target
  "Given a Markdown file (java.io.File),
  return the target HTML file path."
  [md-file]
  (-> (.getPath md-file)
      (#(s/replace % #"src/content" "public"))
      (#(s/replace % #".md" ".html"))))

(defn wrap-post
  "Given a raw HTML object (using hiccup2.core/raw),
  wrap it in an HTML skeleton and return as a string."
  [post-html]
  (str "<!DOCTYPE html>"
       (h/html
        [:html
         page-head
         [:body
          [:main post-html]
          page-menu
          [:script {:src "/main.js"}]]])))

(defn make-content
  "Given a Markdown file (java.io.File),
  return a map with the target HTML file path and content."
  [md-file]
  {:target (post-html-target md-file)
   :content ((comp wrap-post md->hiccup slurp) md-file)})

(defn filter-files
  "Given a file-seq, filter files and not directories."
  [f-seq]
  (filter #(not (.isDirectory %)) f-seq))

(defn -main
  []
  (let [contents (-> (file "src/content/")
                     file-seq
                     filter-files
                     (#(map make-content %)))]
    (sh "find" "public/" "-name" "*.html" "-type" "f" "-delete")
    (println "Removed HTML files in public/")
    (doseq [{target :target content :content} contents]
      (spit target content)
      (println "Wrote " target)))
  (shutdown-agents))
