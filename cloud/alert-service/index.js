const express = require('express');
const admin = require('firebase-admin');
const { PubSub } = require('@google-cloud/pubsub');

// Firebase Admin SDK — usa credenciais do ambiente (Cloud Run injeta automaticamente)
admin.initializeApp();

const app = express();
app.use(express.json());

const pubsub = new PubSub();

// Health check
app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'vigipro-alert-service', version: '1.0.0' });
});

/**
 * POST /api/alert
 * Recebe alerta de camera e envia push notification via FCM.
 * Body: { userId, siteId, cameraName, alertType, message, fcmToken? }
 */
app.post('/api/alert', async (req, res) => {
  try {
    const { userId, siteId, cameraName, alertType, message, fcmToken } = req.body;

    if (!userId || !cameraName || !alertType) {
      return res.status(400).json({ error: 'Campos obrigatorios: userId, cameraName, alertType' });
    }

    // Publica evento no Pub/Sub pra processamento async
    const topic = pubsub.topic('camera-events');
    await topic.publishMessage({
      json: { userId, siteId, cameraName, alertType, message, timestamp: new Date().toISOString() },
    });

    // Envia push notification se tiver FCM token
    if (fcmToken) {
      await admin.messaging().send({
        token: fcmToken,
        notification: {
          title: `Alerta: ${cameraName}`,
          body: message || `${alertType} detectado`,
        },
        data: {
          type: alertType,
          siteId: siteId || '',
          cameraName,
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'vigipro_alerts',
            priority: 'max',
          },
        },
      });
    }

    res.json({ status: 'ok', published: true });
  } catch (error) {
    console.error('Erro ao processar alerta:', error);
    res.status(500).json({ error: 'Erro interno ao processar alerta' });
  }
});

/**
 * POST /api/alert/broadcast
 * Envia push pra todos os membros de um site (via topic FCM).
 * Body: { siteId, cameraName, alertType, message }
 */
app.post('/api/alert/broadcast', async (req, res) => {
  try {
    const { siteId, cameraName, alertType, message } = req.body;

    if (!siteId || !alertType) {
      return res.status(400).json({ error: 'Campos obrigatorios: siteId, alertType' });
    }

    // Envia pra topic FCM do site (usuarios se inscrevem no app)
    await admin.messaging().send({
      topic: `site_${siteId}`,
      notification: {
        title: `Alerta: ${cameraName || 'Camera'}`,
        body: message || `${alertType} detectado`,
      },
      data: {
        type: alertType,
        siteId,
        cameraName: cameraName || '',
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'vigipro_alerts',
          priority: 'max',
        },
      },
    });

    res.json({ status: 'ok', broadcast: true, topic: `site_${siteId}` });
  } catch (error) {
    console.error('Erro ao broadcast:', error);
    res.status(500).json({ error: 'Erro interno ao enviar broadcast' });
  }
});

/**
 * GET /api/health
 * Chamado pelo Cloud Scheduler pra manter o servico warm.
 */
app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    timestamp: new Date().toISOString(),
  });
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`VigiPro Alert Service rodando na porta ${PORT}`);
});
