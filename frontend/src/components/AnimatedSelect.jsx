import {
  Children,
  Fragment,
  isValidElement,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
} from "react";
import { createPortal } from "react-dom";
import "./AnimatedSelect.css";

function obtenerOpciones(children) {
  return Children.toArray(children).flatMap((child) => {
    if (!isValidElement(child)) return [];
    if (child.type === Fragment) return obtenerOpciones(child.props.children);
    if (child.type !== "option") return [];

    return [
      {
        value: String(child.props.value ?? child.props.children ?? ""),
        label: child.props.children,
        texto: String(child.props.children ?? ""),
        disabled: child.props.disabled,
      },
    ];
  });
}

function AnimatedSelect({
  children,
  value,
  defaultValue,
  onChange,
  disabled = false,
  required = false,
  name,
  id,
  className = "",
  style,
  title,
  autoFocus,
  "aria-label": ariaLabel,
  "aria-labelledby": ariaLabelledBy,
  ...nativeProps
}) {
  const generatedId = useId();
  const selectId = id || `animated-select-${generatedId}`;
  const listboxId = `${selectId}-options`;
  const options = obtenerOpciones(children);
  const selectedValue = String(value ?? defaultValue ?? "");
  const selectedIndex = options.findIndex(
    (option) => option.value === selectedValue,
  );
  const triggerRef = useRef(null);
  const nativeRef = useRef(null);
  const wrapperRef = useRef(null);
  const listboxRef = useRef(null);
  const timeoutRef = useRef(null);
  const typeaheadRef = useRef("");
  const typeaheadTimeoutRef = useRef(null);
  const etiquetaRef = useRef("");
  const [abierto, setAbierto] = useState(false);
  const [montado, setMontado] = useState(false);
  const [cerrando, setCerrando] = useState(false);
  const [activo, setActivo] = useState(Math.max(selectedIndex, 0));
  const [etiqueta, setEtiqueta] = useState(ariaLabel || "");
  const [posicion, setPosicion] = useState(null);

  const actualizarPosicion = useCallback(() => {
    const rect = triggerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const separacion = 6;
    const margen = 8;
    const espacioAbajo = window.innerHeight - rect.bottom - separacion - margen;
    const espacioArriba = rect.top - separacion - margen;
    const alturaOpcion = 36;
    const altoContenido = Math.min(options.length * alturaOpcion + 12, 288);
    const arriba =
      espacioAbajo < Math.min(altoContenido, 180) &&
      espacioArriba > espacioAbajo;
    const espacioDisponible = arriba ? espacioArriba : espacioAbajo;
    const altoMaximo = Math.max(0, Math.min(288, espacioDisponible));
    const altoReal = Math.min(altoContenido, altoMaximo);
    const left = Math.max(
      margen,
      Math.min(rect.left, window.innerWidth - rect.width - margen),
    );

    setPosicion({
      top: arriba ? rect.top - separacion - altoReal : rect.bottom + separacion,
      left,
      width: Math.min(rect.width, window.innerWidth - left - margen),
      maxHeight: altoMaximo,
      placement: arriba ? "above" : "below",
    });
  }, [options.length]);

  const primerHabilitado = (desdeFinal = false) => {
    const indices = options
      .map((option, index) => (option.disabled ? -1 : index))
      .filter((index) => index >= 0);
    return desdeFinal ? indices.at(-1) : indices[0];
  };

  const finalizarCierre = () => {
    clearTimeout(timeoutRef.current);
    setAbierto(false);
    setCerrando(true);
    timeoutRef.current = setTimeout(() => {
      setMontado(false);
      setCerrando(false);
    }, 160);
  };

  const abrir = (indice = selectedIndex) => {
    clearTimeout(timeoutRef.current);
    actualizarPosicion();
    setMontado(true);
    setCerrando(false);
    setActivo(
      indice >= 0 && !options[indice]?.disabled
        ? indice
        : (primerHabilitado() ?? 0),
    );
    setAbierto(true);
  };

  const seleccionar = (option) => {
    if (disabled || option.disabled) return;
    if (nativeRef.current) nativeRef.current.value = option.value;
    onChange?.({
      target: nativeRef.current,
      currentTarget: nativeRef.current,
    });
    triggerRef.current?.focus();
    finalizarCierre();
  };

  const manejarTeclado = (event) => {
    if (disabled) return;

    const habilitados = options
      .map((option, index) => (option.disabled ? -1 : index))
      .filter((index) => index >= 0);
    if (!habilitados.length) return;

    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const direccion = event.key === "ArrowDown" ? 1 : -1;
      if (!abierto) {
        abrir(
          event.key === "ArrowDown"
            ? (selectedIndex >= 0 ? selectedIndex : primerHabilitado())
            : (selectedIndex >= 0
                ? selectedIndex
                : primerHabilitado(true)),
        );
        return;
      }
      const siguiente = habilitados.findIndex((index) => index === activo);
      const posicion = (siguiente + direccion + habilitados.length) % habilitados.length;
      setActivo(habilitados[posicion]);
      return;
    }

    if (!abierto && (event.key === "Enter" || event.key === " ")) {
      event.preventDefault();
      abrir();
      return;
    }

    if (!abierto) return;

    if (event.key === "Escape") {
      event.preventDefault();
      finalizarCierre();
    } else if (event.key === "Home" || event.key === "End") {
      event.preventDefault();
      setActivo(
        event.key === "Home" ? primerHabilitado() : primerHabilitado(true),
      );
    } else if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      const option = options[activo];
      if (option) seleccionar(option);
    } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey) {
      typeaheadRef.current += event.key.toLocaleLowerCase();
      clearTimeout(typeaheadTimeoutRef.current);
      typeaheadTimeoutRef.current = setTimeout(() => {
        typeaheadRef.current = "";
      }, 500);
      const inicio = habilitados.indexOf(activo);
      const ordenados = [
        ...habilitados.slice(inicio + 1),
        ...habilitados.slice(0, inicio + 1),
      ];
      const coincidencia = ordenados.find((index) =>
        options[index].texto.toLocaleLowerCase().startsWith(typeaheadRef.current),
      );
      if (coincidencia !== undefined) setActivo(coincidencia);
    }
  };

  useEffect(() => {
    if (!abierto) return undefined;
    const cerrarFuera = (event) => {
      if (
        !wrapperRef.current?.contains(event.target) &&
        !listboxRef.current?.contains(event.target)
      )
        finalizarCierre();
    };
    document.addEventListener("pointerdown", cerrarFuera);
    window.addEventListener("resize", actualizarPosicion);
    window.addEventListener("scroll", actualizarPosicion, true);
    return () => {
      document.removeEventListener("pointerdown", cerrarFuera);
      window.removeEventListener("resize", actualizarPosicion);
      window.removeEventListener("scroll", actualizarPosicion, true);
    };
  }, [abierto, actualizarPosicion]);

  useEffect(() => {
    if (!abierto) return;
    document
      .getElementById(`${selectId}-option-${activo}`)
      ?.scrollIntoView({ block: "nearest" });
  }, [abierto, activo, selectId]);

  useEffect(
    () => () => {
      clearTimeout(timeoutRef.current);
      clearTimeout(typeaheadTimeoutRef.current);
    },
    [],
  );

  const asignarWrapper = useCallback(
    (node) => {
      wrapperRef.current = node;
      if (!node || ariaLabel) return;
      const label = node.closest("label");
      if (!label) return;

      const textos = [];
      const recorrer = (parent) => {
        for (const child of parent.childNodes) {
          if (child === node) continue;
          if (child.nodeType === Node.TEXT_NODE) textos.push(child.textContent);
          else if (child.nodeType === Node.ELEMENT_NODE) recorrer(child);
        }
      };
      recorrer(label);
      const siguienteEtiqueta = textos.join(" ").replace(/\s+/g, " ").trim();
      if (siguienteEtiqueta && siguienteEtiqueta !== etiquetaRef.current) {
        etiquetaRef.current = siguienteEtiqueta;
        setEtiqueta(siguienteEtiqueta);
      }
    },
    [ariaLabel],
  );

  const seleccionado = options[selectedIndex];

  return (
    <div
      ref={asignarWrapper}
      className={`animated-select ${className}`.trim()}
      style={style}
    >
      <select
        {...nativeProps}
        ref={nativeRef}
        id={`${selectId}-native`}
        className="animated-select__native"
        value={value}
        defaultValue={value === undefined ? defaultValue : undefined}
        name={name}
        required={required}
        disabled={disabled}
        tabIndex={-1}
        aria-hidden="true"
        onChange={() => {}}
        onInvalid={(event) => {
          event.preventDefault();
          triggerRef.current?.focus();
        }}
      >
        {children}
      </select>

      <button
        ref={triggerRef}
        type="button"
        id={selectId}
        className="animated-select__trigger"
        disabled={disabled}
        aria-label={ariaLabel || etiqueta || undefined}
        aria-labelledby={ariaLabelledBy}
        aria-haspopup="listbox"
        aria-expanded={abierto}
        aria-controls={listboxId}
        aria-activedescendant={
          abierto ? `${selectId}-option-${activo}` : undefined
        }
        aria-required={required || undefined}
        title={title}
        autoFocus={autoFocus}
        onClick={(event) => {
          event.preventDefault();
          if (abierto) finalizarCierre();
          else abrir();
        }}
        onKeyDown={manejarTeclado}
      >
        <span className="animated-select__value">
          {seleccionado?.label ?? "\u00a0"}
        </span>
        <span className="animated-select__icon" aria-hidden="true" />
      </button>

      {montado && (
        createPortal(
          <div
            ref={listboxRef}
            id={listboxId}
            className={`animated-select__listbox${abierto ? " is-open" : ""}${cerrando ? " is-closing" : ""}`}
            role="listbox"
            data-placement={posicion?.placement}
            style={
              posicion
                ? {
                    top: posicion.top,
                    left: posicion.left,
                    width: posicion.width,
                    maxHeight: posicion.maxHeight,
                  }
                : undefined
            }
            aria-label={ariaLabel || etiqueta || undefined}
            aria-labelledby={ariaLabelledBy}
            onAnimationEnd={(event) => {
              if (cerrando && event.target === event.currentTarget) {
                clearTimeout(timeoutRef.current);
                setMontado(false);
                setCerrando(false);
              }
            }}
          >
            {options.map((option, index) => (
              <div
                key={`${option.value}-${index}`}
                id={`${selectId}-option-${index}`}
                className={`animated-select__option${index === selectedIndex ? " is-selected" : ""}${index === activo && abierto ? " is-active" : ""}${option.disabled ? " is-disabled" : ""}`}
                role="option"
                aria-selected={index === selectedIndex}
                aria-disabled={option.disabled || undefined}
                onPointerEnter={() => !option.disabled && setActivo(index)}
                onClick={() => seleccionar(option)}
              >
                {option.label}
              </div>
            ))}
          </div>,
          document.body,
        )
      )}
    </div>
  );
}

export default AnimatedSelect;
