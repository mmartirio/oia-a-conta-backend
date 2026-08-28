package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.entity.AutomacaoMensagem;
import com.oiaaconta.whatsapp.repository.AutomacaoMensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutomacaoMensagemService {

    private final AutomacaoMensagemRepository repository;

    public List<AutomacaoMensagem> listar(Long restauranteId) {
        return repository.findByRestauranteIdOrderByCreatedAtDesc(restauranteId);
    }

    @Transactional
    public AutomacaoMensagem criar(Long restauranteId, String acionador, String mensagem) {
        validar(acionador, mensagem);
        return repository.save(AutomacaoMensagem.builder()
            .restauranteId(restauranteId)
            .acionador(acionador.trim())
            .mensagem(mensagem.trim())
            .ativo(true)
            .build());
    }

    @Transactional
    public AutomacaoMensagem atualizar(Long restauranteId, Long id, String acionador, String mensagem, Boolean ativo) {
        validar(acionador, mensagem);
        AutomacaoMensagem a = buscar(restauranteId, id);
        a.setAcionador(acionador.trim());
        a.setMensagem(mensagem.trim());
        if (ativo != null) a.setAtivo(ativo);
        return repository.save(a);
    }

    @Transactional
    public void remover(Long restauranteId, Long id) {
        repository.delete(buscar(restauranteId, id));
    }

    // Match exato (normalizado) — evita respostas automáticas disparando por
    // engano em meio a uma frase qualquer do cliente.
    public Optional<AutomacaoMensagem> encontrarPorAcionador(Long restauranteId, String textoNormalizado) {
        return repository.findByRestauranteIdAndAtivoTrue(restauranteId).stream()
            .filter(a -> a.getAcionador().trim().equalsIgnoreCase(textoNormalizado))
            .findFirst();
    }

    private void validar(String acionador, String mensagem) {
        if (acionador == null || acionador.isBlank()) {
            throw new IllegalArgumentException("Informe o texto que o cliente precisa enviar (acionador)");
        }
        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("Informe a mensagem de resposta");
        }
    }

    private AutomacaoMensagem buscar(Long restauranteId, Long id) {
        return repository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new NoSuchElementException("Automação não encontrada"));
    }
}
