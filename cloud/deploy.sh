#!/bin/bash
set -e

SERVER="root@72.60.9.175"
DEPLOY_DIR="/opt/vigipro/cloud"
DOMAIN="vigipro.mahina.cloud"

echo "🚀 Deploy VigiPro Cloud → $DOMAIN"

# 1. Cria diretório no servidor
echo "→ Preparando servidor..."
ssh $SERVER "mkdir -p $DEPLOY_DIR"

# 2. Sincroniza código (exclui artefatos de build)
echo "→ Sincronizando código..."
rsync -avz --progress \
  --exclude='.git' \
  --exclude='_build' \
  --exclude='deps' \
  --exclude='node_modules' \
  --exclude='.env' \
  ./ $SERVER:$DEPLOY_DIR/

# 3. Verifica se .env existe no servidor
echo "→ Verificando variáveis de ambiente..."
ssh $SERVER "test -f $DEPLOY_DIR/.env || (echo 'ERRO: Crie o .env no servidor antes de fazer deploy!' && echo 'Copie .env.example e preencha os valores.' && exit 1)"

# 4. Build da imagem Docker
echo "→ Buildando imagem (pode demorar alguns minutos)..."
ssh $SERVER "cd $DEPLOY_DIR && docker compose build --no-cache"

# 5. Restart do serviço
echo "→ Subindo serviço..."
ssh $SERVER "cd $DEPLOY_DIR && docker compose up -d"

# 6. Configura Nginx (só se não existir)
echo "→ Configurando Nginx..."
ssh $SERVER "test -f /etc/nginx/sites-available/$DOMAIN || cat > /etc/nginx/sites-available/$DOMAIN << 'NGINX'
server {
    server_name $DOMAIN;

    # HLS streaming — sem buffer para baixa latência
    location /hls/ {
        proxy_pass http://localhost:4000;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Cache-Control' 'no-cache' always;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300;
    }

    location / {
        proxy_pass http://localhost:4000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_cache_bypass \$http_upgrade;
        proxy_read_timeout 300;
        proxy_send_timeout 300;
    }

    listen 80;
}
NGINX"

ssh $SERVER "ln -sf /etc/nginx/sites-available/$DOMAIN /etc/nginx/sites-enabled/$DOMAIN 2>/dev/null || true"
ssh $SERVER "nginx -t && systemctl reload nginx"

# 7. SSL com Certbot
echo "→ Obtendo certificado SSL..."
ssh $SERVER "certbot --nginx -d $DOMAIN --non-interactive --agree-tos -m admin@mahina.cloud 2>/dev/null || echo 'SSL já configurado ou falhou (DNS pode não ter propagado)'"

# 8. Status final
echo ""
echo "✅ Deploy concluído!"
echo ""
ssh $SERVER "cd $DEPLOY_DIR && docker compose ps"
echo ""
echo "🔗 https://$DOMAIN/api/health"
