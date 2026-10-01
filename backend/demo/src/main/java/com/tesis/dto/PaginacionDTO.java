package com.tesis.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public class PaginacionDTO {

    public static class Solicitud {
        private int page;
        private int size = 25;

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public Pageable toPageable() {
            if (page < 0 || size < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "page debe ser mayor o igual a 0 y size debe ser mayor que 0");
            }
            return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "id"));
        }
    }

    public record Respuesta<T>(List<T> content,
                               int page,
                               int size,
                               long totalElements,
                               int totalPages,
                               boolean first,
                               boolean last) {
        public static <T> Respuesta<T> desde(Page<T> pagina) {
            return new Respuesta<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                    pagina.getTotalElements(), pagina.getTotalPages(), pagina.isFirst(), pagina.isLast());
        }
    }
}