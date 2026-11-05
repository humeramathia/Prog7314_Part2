const {
  topicForSport,
  fixtureMessage,
  notifyFixture,
  createFcmNotifier,
  createMemoryNotifier
} = require("../src/notify");

describe("topicForSport", () => {
  test("prefixes the sport id and fixture type", () => {
    expect(topicForSport("football", "PRACTICE")).toBe("sport_football_practice");
    expect(topicForSport("football", "SOCIAL_EVENT")).toBe("sport_football_event");
    expect(topicForSport("football", "ANNOUNCEMENT")).toBe("sport_football_announcement");
  });

  test("replaces characters FCM does not allow in topic names", () => {
    expect(topicForSport(" Ultimate Frisbee! ", "PRACTICE")).toBe("sport_ultimate_frisbee__practice");
  });

  test("returns an empty string when there is no sport", () => {
    expect(topicForSport("")).toBe("");
    expect(topicForSport(undefined)).toBe("");
  });
});

describe("fixtureMessage", () => {
  const event = {
    id: "evt-1",
    sportId: "archery",
    title: "Range practice",
    type: "PRACTICE",
    startsAt: Date.UTC(2026, 10, 5, 14, 0),
    location: "Main field"
  };

  test("names the sport, type and action in the title", () => {
    expect(fixtureMessage(event, "Archery").title).toBe("Archery: Practice added");
    expect(fixtureMessage(event, "Archery", "updated").title).toBe("Archery: Practice updated");
  });

  test("includes the fixture title and venue in the body", () => {
    const { body } = fixtureMessage(event, "Archery");
    expect(body).toContain("Range practice");
    expect(body).toContain("at Main field");
  });

  test("carries string-only data so the app can read it", () => {
    const { data } = fixtureMessage(event, "Archery");
    expect(data).toEqual({ kind: "fixture", action: "created", eventId: "evt-1", sportId: "archery" });
    Object.values(data).forEach((value) => expect(typeof value).toBe("string"));
  });
});

describe("notifiers", () => {
  test("FCM notifier skips sending when Firebase is not configured", async () => {
    const notifier = createFcmNotifier(() => null);
    await expect(notifier.toTopic("sport_football", { title: "t", body: "b" })).resolves.toEqual({
      sent: false,
      reason: "fcm-not-configured"
    });
  });

  test("FCM notifier sends to the Android fixtures channel", async () => {
    const send = jest.fn().mockResolvedValue("projects/x/messages/1");
    const notifier = createFcmNotifier(() => ({ send }));
    const result = await notifier.toToken("device-token", { title: "t", body: "b", data: { kind: "test" } });
    expect(result).toEqual({ sent: true, id: "projects/x/messages/1" });
    expect(send).toHaveBeenCalledWith(
      expect.objectContaining({
        token: "device-token",
        notification: { title: "t", body: "b" },
        android: expect.objectContaining({ notification: { channelId: "fixtures" } })
      })
    );
  });

  test("notifyFixture never throws when the push fails", async () => {
    const notifier = { toTopic: jest.fn().mockRejectedValue(new Error("quota")) };
    const result = await notifyFixture(notifier, { sportId: "football", title: "x", startsAt: 0 }, "Football");
    expect(result.sent).toBe(false);
  });

  test("memory notifier records topic messages", async () => {
    const notifier = createMemoryNotifier();
    await notifyFixture(notifier, { id: "e", sportId: "netball", title: "Match", type: "SOCIAL_EVENT", startsAt: 0 }, "Netball");
    expect(notifier.sent).toHaveLength(1);
    expect(notifier.sent[0].topic).toBe("sport_netball_event");
  });
});
