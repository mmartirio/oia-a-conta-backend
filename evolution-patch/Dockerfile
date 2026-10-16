# main.js = dist/main.js da v2.2.3 com um patch pequeno (envio pra contatos @lid
# sem consultar o número). A versão TEM que ser a mesma do main.js — com
# :latest, uma imagem nova recebia o main.js antigo por cima (quebra e desfaz
# correções de segurança). Pra atualizar a Evolution, refazer o patch na nova versão.
FROM evoapicloud/evolution-api:v2.2.3
COPY main.js /evolution/dist/main.js
