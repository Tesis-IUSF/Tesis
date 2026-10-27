import { useEffect, useRef, useState } from "react";

// Devuelve [ref, visible]: "visible" pasa a true una sola vez, cuando el
// elemento entra en pantalla. Sirve para animar secciones al hacer scroll.
export default function useAparecer(umbral = 0.2) {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el || typeof IntersectionObserver === "undefined") {
      setVisible(true);
      return undefined;
    }
    const obs = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          setVisible(true);
          obs.disconnect();
        }
      },
      { threshold: umbral },
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, [umbral]);

  return [ref, visible];
}
