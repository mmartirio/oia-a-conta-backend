package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {
    List<ItemCarrinho> findBySessaoId(Long sessaoId);
    void deleteBySessaoId(Long sessaoId);
}
