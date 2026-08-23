package com.oiaaconta.auth.service;

import com.oiaaconta.auth.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Bloqueio temporário de login por e-mail, em memória — sem isso, o login não
// tinha nenhum limite de tentativas, tornando a senha brute-forceável. Leve
// de propósito (sem Redis/infra nova): um bloqueio por instância já cobre o
// caso real (um atacante martelando o mesmo e-mail), e o estado zera se o
// serviço reiniciar, o que é aceitável pra esse tipo de proteção.
@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private record Estado(int tentativas, Instant primeiraTentativa, Instant bloqueadoAte) { }

    private final ConcurrentHashMap<String, Estado> porEmail = new ConcurrentHashMap<>();

    public void verificarBloqueio(String email) {
        Estado e = porEmail.get(chave(email));
        if (e != null && e.bloqueadoAte() != null && Instant.now().isBefore(e.bloqueadoAte())) {
            long minutos = Math.max(1, Duration.between(Instant.now(), e.bloqueadoAte()).toMinutes() + 1);
            throw new BusinessException("Muitas tentativas de login. Tente novamente em " + minutos + " minuto(s).");
        }
    }

    public void registrarFalha(String email) {
        porEmail.compute(chave(email), (k, e) -> {
            Instant agora = Instant.now();
            if (e == null || Duration.between(e.primeiraTentativa(), agora).compareTo(JANELA) > 0) {
                return new Estado(1, agora, null);
            }
            int novasTentativas = e.tentativas() + 1;
            Instant bloqueadoAte = novasTentativas >= MAX_TENTATIVAS ? agora.plus(BLOQUEIO) : null;
            return new Estado(novasTentativas, e.primeiraTentativa(), bloqueadoAte);
        });
    }

    public void registrarSucesso(String email) {
        porEmail.remove(chave(email));
    }

    private String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
