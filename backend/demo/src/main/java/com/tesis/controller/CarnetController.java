package com.tesis.controller;

import com.tesis.dto.CarnetDTO.EmpleadoCarnetResponseDTO;
import com.tesis.dto.CarnetDTO.GeneracionLoteRequestDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.service.CarnetConsultaService;
import com.tesis.service.CarnetLoteService;
import com.tesis.service.CarnetService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empleados")
public class CarnetController {

    private final CarnetService carnetService;
    private final CarnetConsultaService carnetConsultaService;
    private final CarnetLoteService carnetLoteService;

    public CarnetController(CarnetService carnetService,
                            CarnetConsultaService carnetConsultaService,
                            CarnetLoteService carnetLoteService) {
        this.carnetService = carnetService;
        this.carnetConsultaService = carnetConsultaService;
        this.carnetLoteService = carnetLoteService;
    }

    @GetMapping("/carnets")
    public PaginacionDTO.Respuesta<EmpleadoCarnetResponseDTO> listarParaCarnet(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer departamentoId,
            @RequestParam(required = false) Integer cargoId,
            @RequestParam(required = false) String estadoQr,
            @RequestParam(defaultValue = "30") int diasAviso,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        return PaginacionDTO.Respuesta.desde(carnetConsultaService.buscar(
                q, departamentoId, cargoId, estadoQr, diasAviso, pageable));
    }

    @PostMapping("/carnets/lote")
    public ResponseEntity<byte[]> generarLote(@Valid @RequestBody GeneracionLoteRequestDTO request) {
        boolean zip = "zip".equalsIgnoreCase(request.getFormato());
        byte[] archivo = carnetLoteService.generar(request.getEmpleadoIds(), request.getFormato());
        String nombre = zip ? "carnets-personal.zip" : "carnets-personal.pdf";
        return ResponseEntity.ok()
                .contentType(zip ? MediaType.parseMediaType("application/zip") : MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nombre).build().toString())
                .body(archivo);
    }

    @PostMapping("/carnets/lote/descarga")
    public ResponseEntity<byte[]> descargarLote(@Valid @RequestBody GeneracionLoteRequestDTO request) {
        boolean zip = "zip".equalsIgnoreCase(request.getFormato());
        byte[] archivo = carnetLoteService.descargarExistentes(
                request.getEmpleadoIds(), request.getFormato());
        String nombre = zip ? "carnets-personal.zip" : "carnets-personal.pdf";
        return ResponseEntity.ok()
                .contentType(zip ? MediaType.parseMediaType("application/zip") : MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nombre).build().toString())
                .body(archivo);
    }

    @PostMapping(value = "/{id}/carnet", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarCarnet(@PathVariable Integer id) {
        byte[] pdf = carnetService.generarCarnet(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("carnet-empleado-" + id + ".pdf").build().toString())
                .body(pdf);
    }
}