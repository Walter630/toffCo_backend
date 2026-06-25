package com.site.toffCo.module.pagamentoitem.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_pagamentoitem")
public class PagamentoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
