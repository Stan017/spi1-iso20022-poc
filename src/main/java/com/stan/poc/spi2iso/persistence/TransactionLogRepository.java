package com.stan.poc.spi2iso.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    Optional<TransactionLog> findByNumeroInstruccion(String numeroInstruccion);
}
