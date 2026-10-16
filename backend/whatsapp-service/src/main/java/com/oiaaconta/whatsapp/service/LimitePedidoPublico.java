package com.oiaaconta.whatsapp.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Limite do pedido pelo cardápio público (sem login): cada pedido dispara
// mensagem de WhatsApp pelo número do restaurante pro telefone informado —
// sem limite, dava pra usar isso pra mandar spam a qualquer número (e ter o
// número do restaurante banido) ou inundar a cozinha de pedidos falsos.
// Em memória: suficiente pra uma instância só do whatsapp-service.
@Component
public class LimitePedidoPublico {

    private static final int MAX_POR_TELEFONE = 3;
    private static final long JANELA_TELEFONE_MS = 10 * 60 * 1000L;
    private static final int MAX_POR_IP = 10;
    private static final long JANELA_IP_MS = 60 * 60 * 1000L;

    private final Map<String, Deque<Long>> porChave = new ConcurrentHashMap<>();

    public boolean permitir(String telefone, String ip) {
        long agora = System.currentTimeMillis();
        if (porChave.size() > 50_000) porChave.clear();
        return registrar("tel:" + telefone, MAX_POR_TELEFONE, JANELA_TELEFONE_MS, agora)
            & (ip == null || registrar("ip:" + ip, MAX_POR_IP, JANELA_IP_MS, agora));
    }

    private boolean registrar(String chave, int max, long janelaMs, long agora) {
        Deque<Long> fila = porChave.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() > janelaMs) fila.pollFirst();
            if (fila.size() >= max) return false;
            fila.addLast(agora);
            return true;
        }
    }
}
