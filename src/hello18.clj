(ns hello18
  (:import [javax.swing JFrame Timer JPanel]
           [java.awt Font Color Graphics Graphics2D RenderingHints GradientPaint Toolkit Point Cursor AlphaComposite]
           [java.awt.event MouseAdapter ActionListener]
           [java.awt.image BufferedImage]
           [java.awt GraphicsEnvironment]))

(def pomodoro-durations [120 300 600 1500])
(def pomodoro-colors [(Color. 173 216 230)
                      (Color. 152 251 152)
                      (Color. 255 239 150)
                      (Color. 255 182 193)])

(def grid-cols 8)
(def fade-duration 500) ;; ms fade-in

(defn format-time [seconds]
  (format "%02d:%02d" (quot seconds 60) (mod seconds 60)))

(defn brighter [^Color c factor]
  (let [r (int (+ (* (- 255 (.getRed c)) factor) (.getRed c)))
        g (int (+ (* (- 255 (.getGreen c)) factor) (.getGreen c)))
        b (int (+ (* (- 255 (.getBlue c)) factor) (.getBlue c)))]
    (Color. r g b)))

(defn -main []
  (let [frame (JFrame. "Pomodoro Grid")
        gd (.getDefaultScreenDevice (GraphicsEnvironment/getLocalGraphicsEnvironment))
        timer-seconds (atom (first pomodoro-durations))
        current-mode (atom 0)
        running? (atom false)
        blink? (atom false)
        countdown-timer (atom nil)
        blink-timer (atom nil)
        display-timer (atom nil)
        tile-activation (atom [])
        last-x (atom nil)
        last-tap (atom 0)]

    ;; Panel
    (let [panel (proxy [JPanel] []
                  (paintComponent [^Graphics g]
                    (proxy-super paintComponent g)
                    (let [g2 (.create ^Graphics2D g)
                          w (int (.getWidth this))
                          h (int (.getHeight this))
                          total-time (nth pomodoro-durations @current-mode)
                          elapsed (- total-time @timer-seconds)
                          percent (/ elapsed (double total-time))
                          base-color (nth pomodoro-colors @current-mode)
                          text (if (and @running? @blink?) "" (format-time @timer-seconds))
                          tile-size (int (/ w grid-cols))
                          grid-rows (int (Math/ceil (/ h (double tile-size))))
                          total-cells (* grid-cols grid-rows)
                          now (System/currentTimeMillis)]

                      ;; Ensure tile activation timestamps are filled as needed
                      (let [needed (int (* total-cells percent))]
                        (swap! tile-activation
                               (fn [acts]
                                 (loop [acts (vec acts) i (count acts)]
                                   (if (< i needed)
                                     (recur (conj acts now) (inc i))
                                     acts)))))

                      ;; Background
                      (.setColor g2 Color/BLACK)
                      (.fillRect g2 0 0 w h)

                      ;; Draw grid
                      (doseq [i (range total-cells)]
                        (let [row (int (/ i grid-cols))
                              col (mod i grid-cols)
                              x (* col tile-size)
                              y (* row tile-size)
                              start-time (get @tile-activation i)
                              alpha (if start-time
                                      (min 1.0 (/ (- now start-time) (double fade-duration)))
                                      0.0)]
                          (when (> alpha 0)
                            (let [grad (GradientPaint. x y
                                                       (brighter base-color 0.3)
                                                       x (+ y tile-size)
                                                       base-color)]
                              (.setPaint g2 grad)
                              (.setComposite g2 (AlphaComposite/getInstance AlphaComposite/SRC_OVER (float alpha)))
                              (.fillRoundRect g2 x y (dec tile-size) (dec tile-size) 20 20)))))

                      ;; Reset composite
                      (.setComposite g2 (AlphaComposite/getInstance AlphaComposite/SRC_OVER 1.0))

                      ;; Timer text
                      (.setRenderingHint g2 RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                      (.setFont g2 (Font. "SansSerif" Font/BOLD 80))
                      (let [metrics (.getFontMetrics g2)
                            text-width (.stringWidth metrics text)
                            text-height (.getAscent metrics)]
                        (.setColor g2 Color/WHITE)
                        (.drawString g2 text
                                     (int (- (/ w 2) (/ text-width 2)))
                                     (int (+ (/ h 2) (/ text-height 3)))))
                      (.dispose g2))))]

      ;; Helper repaint
      (defn repaint! [] (.repaint panel))

      ;; Timers
      (letfn [(start-display-loop []
                (reset! display-timer
                        (Timer. 33 ;; ~30 FPS
                                (proxy [ActionListener] []
                                  (actionPerformed [_] (repaint!)))))
                (.start ^Timer @display-timer))

              (start-blink []
                (reset! blink-timer
                        (Timer. 500
                                (proxy [ActionListener] []
                                  (actionPerformed [_]
                                    (swap! blink? not)
                                    (repaint!)))))
                (.start ^Timer @blink-timer))

              (start-timer []
                (reset! running? true)
                (reset! tile-activation [])
                (reset! countdown-timer
                        (Timer. 1000
                                (proxy [ActionListener] []
                                  (actionPerformed [_]
                                    (if (pos? @timer-seconds)
                                      (do
                                        (swap! timer-seconds dec))
                                      (do
                                        (.stop ^Timer @countdown-timer)
                                        (reset! running? false)
                                        (start-blink)))))))
                (.start ^Timer @countdown-timer))

              (switch-mode [dir]
                (reset! current-mode (mod (+ @current-mode dir) (count pomodoro-durations)))
                (reset! timer-seconds (nth pomodoro-durations @current-mode))
                (reset! tile-activation [])
                (reset! running? false)
                (reset! blink? false)
                (when-let [t @countdown-timer] (.stop ^Timer t))
                (when-let [b @blink-timer] (.stop ^Timer b))
                (repaint!))]

        ;; Gestures
        (let [touch-handler
              (proxy [MouseAdapter] []
                (mousePressed [e]
                  (reset! last-x (.getX e))
                  (let [now (System/currentTimeMillis)]
                    (when (< (- now @last-tap) 300)
                      (when (not @running?)
                        (start-timer)))
                    (reset! last-tap now)))
                (mouseReleased [e]
                  (let [dx (- (.getX e) @last-x)]
                    (cond
                      (> dx 50) (switch-mode -1)
                      (< dx -50) (switch-mode 1)))))]
          (.addMouseListener panel touch-handler)
          (.addMouseMotionListener panel touch-handler))

        ;; Frame setup
        (.add frame panel)
        (.setUndecorated frame true)
        (.setVisible frame true)
        (.setFullScreenWindow gd frame)

        ;; Hide cursor
        (let [blank-cursor (.createCustomCursor
                             (Toolkit/getDefaultToolkit)
                             (BufferedImage. 1 1 BufferedImage/TYPE_INT_ARGB)
                             (Point. 0 0)
                             "blank")]
          (.setCursor frame blank-cursor))

        (start-display-loop)))))
