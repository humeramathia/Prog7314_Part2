const { CosmosClient } = require("@azure/cosmos");
const { config } = require("../config");

const CONTAINERS = {
  users: "/userId",
  sports: "/sportId",
  events: "/sportId",
  performance: "/userId",
  learning: "/sportId"
};

async function queryAll(container, query, parameters = [], partitionKey) {
  const options = partitionKey ? { partitionKey } : {};
  const { resources } = await container.items
    .query({ query, parameters }, options)
    .fetchAll();
  return resources;
}

async function createCosmosStore() {
  const client = new CosmosClient({
    endpoint: config.cosmos.endpoint,
    key: config.cosmos.key
  });
  const { database } = await client.databases.createIfNotExists({ id: config.cosmos.database });

  const handles = {};
  for (const [id, path] of Object.entries(CONTAINERS)) {
    const { container } = await database.containers.createIfNotExists({
      id,
      partitionKey: { paths: [path] }
    });
    handles[id] = container;
  }

  return {
    kind: "cosmos",

    async getUser(userId) {
      try {
        const { resource } = await handles.users.item(userId, userId).read();
        return resource || null;
      } catch (err) {
        if (err.code === 404) return null;
        throw err;
      }
    },
    async upsertUser(user) {
      const { resource } = await handles.users.items.upsert(user);
      return resource;
    },

    async listSports() {
      return queryAll(handles.sports, "SELECT * FROM c");
    },
    async getSport(sportId) {
      try {
        const { resource } = await handles.sports.item(sportId, sportId).read();
        return resource || null;
      } catch (err) {
        if (err.code === 404) return null;
        throw err;
      }
    },
    async upsertSport(sport) {
      const { resource } = await handles.sports.items.upsert(sport);
      return resource;
    },

    async listEvents(sportId, from, to) {
      let query = "SELECT * FROM c WHERE c.sportId = @sportId";
      const parameters = [{ name: "@sportId", value: sportId }];
      if (from != null) {
        query += " AND c.startsAt >= @from";
        parameters.push({ name: "@from", value: from });
      }
      if (to != null) {
        query += " AND c.startsAt < @to";
        parameters.push({ name: "@to", value: to });
      }
      query += " ORDER BY c.startsAt ASC";
      return queryAll(handles.events, query, parameters, sportId);
    },
    async getEvent(eventId) {
      const rows = await queryAll(
        handles.events,
        "SELECT * FROM c WHERE c.id = @id",
        [{ name: "@id", value: eventId }]
      );
      return rows[0] || null;
    },
    async upsertEvent(event) {
      const { resource } = await handles.events.items.upsert(event);
      return resource;
    },
    async deleteEvent(eventId) {
      const existing = await this.getEvent(eventId);
      if (!existing) return false;
      await handles.events.item(eventId, existing.sportId).delete();
      return true;
    },

    async listPerformance(userId, sportId) {
      let query = "SELECT * FROM c WHERE c.userId = @userId";
      const parameters = [{ name: "@userId", value: userId }];
      if (sportId) {
        query += " AND c.sportId = @sportId";
        parameters.push({ name: "@sportId", value: sportId });
      }
      query += " ORDER BY c.recordedAt DESC";
      return queryAll(handles.performance, query, parameters, userId);
    },
    async getPerformance(sessionId) {
      const rows = await queryAll(
        handles.performance,
        "SELECT * FROM c WHERE c.id = @id",
        [{ name: "@id", value: sessionId }]
      );
      return rows[0] || null;
    },
    async upsertPerformance(session) {
      const { resource } = await handles.performance.items.upsert(session);
      return resource;
    },
    async deletePerformance(sessionId) {
      const existing = await this.getPerformance(sessionId);
      if (!existing) return false;
      await handles.performance.item(sessionId, existing.userId).delete();
      return true;
    },

    async listLearning(sportId, category) {
      let query = "SELECT * FROM c WHERE c.sportId = @sportId";
      const parameters = [{ name: "@sportId", value: sportId }];
      if (category) {
        query += " AND c.category = @category";
        parameters.push({ name: "@category", value: category });
      }
      return queryAll(handles.learning, query, parameters, sportId);
    },
    async getLearning(guideId) {
      const rows = await queryAll(
        handles.learning,
        "SELECT * FROM c WHERE c.id = @id",
        [{ name: "@id", value: guideId }]
      );
      return rows[0] || null;
    },
    async upsertLearning(guide) {
      const { resource } = await handles.learning.items.upsert(guide);
      return resource;
    }
  };
}

module.exports = { createCosmosStore };
