function hours(n) {
  return n * 3_600_000;
}

function days(n) {
  return n * 86_400_000;
}

function seedEvents(now = Date.now()) {
  return [
    {
      id: "football-set-piece",
      sportId: "football",
      title: "Set-piece clinic",
      type: "PRACTICE",
      startsAt: now + days(2),
      endsAt: now + days(2) + hours(2),
      location: "North pitch, campus fields",
      description: "Corners, free-kick routines, and defensive zonal marks.",
      notes: "Bring both colour bibs. Warm-up on the far touchline at 16:45.",
      createdBy: "seed"
    },
    {
      id: "football-derby",
      sportId: "football",
      title: "Varsity derby vs City FC",
      type: "SOCIAL_EVENT",
      startsAt: now + days(9),
      endsAt: now + days(9) + hours(2),
      location: "Main stadium, west stand",
      description: "First-team fixture. Supporters meet at the clubhouse first.",
      notes: "Be in the changing room 75 minutes before kick-off.",
      createdBy: "seed"
    },
    {
      id: "football-refs",
      sportId: "football",
      title: "Referee briefing: new handball guidance",
      type: "ANNOUNCEMENT",
      startsAt: now + days(4),
      location: "Lecture theatre 2",
      description: "Club referees walk through this season's handball and VAR-style review notes.",
      notes: "Bring a notebook. Session is 40 minutes.",
      createdBy: "seed"
    },
    {
      id: "football-recovery",
      sportId: "football",
      title: "Recovery pool session",
      type: "PRACTICE",
      startsAt: now + days(11),
      endsAt: now + days(11) + hours(1),
      location: "Campus pool, shallow end",
      description: "Easy water jogging and mobility the morning after the derby.",
      notes: "Cap and flip-flops required. No diving.",
      createdBy: "seed"
    },
    {
      id: "cricket-nets",
      sportId: "cricket",
      title: "Pace bowling nets",
      type: "PRACTICE",
      startsAt: now + days(1),
      endsAt: now + days(1) + hours(2),
      location: "Indoor school, lane 3",
      description: "New-ball spell simulation with a radar gun on the bowling arm.",
      notes: "Helmets compulsory, including for throw-downs.",
      createdBy: "seed"
    },
    {
      id: "cricket-t20",
      sportId: "cricket",
      title: "Inter-faculty T20 on the Oval",
      type: "SOCIAL_EVENT",
      startsAt: now + days(8),
      endsAt: now + days(8) + hours(4),
      location: "University Oval",
      description: "Engineering vs Commerce. White ball, two drinks breaks.",
      notes: "Coloured kit. Boundary riders bring sunscreen.",
      createdBy: "seed"
    },
    {
      id: "cricket-pitch",
      sportId: "cricket",
      title: "Pitch inspection — Saturday washed out?",
      type: "ANNOUNCEMENT",
      startsAt: now + days(5),
      location: "Oval groundsman's hut",
      description: "Captain and coach check cover and outfield after overnight rain.",
      notes: "Decision posted on the team chat by 08:00.",
      createdBy: "seed"
    },
    {
      id: "cricket-fielding",
      sportId: "cricket",
      title: "Close-catching and run-out drills",
      type: "PRACTICE",
      startsAt: now + days(12),
      endsAt: now + days(12) + hours(1.5),
      location: "Boundary nets",
      description: "Short-leg catches and one-stump run-out under fatigue.",
      notes: "Inners required. No phones on the square.",
      createdBy: "seed"
    },
    {
      id: "tennis-serve",
      sportId: "tennis",
      title: "First-serve percentage hour",
      type: "PRACTICE",
      startsAt: now + days(3),
      endsAt: now + days(3) + hours(1),
      location: "Courts 5–6",
      description: "Target serving: T, body, and wide, then live second-serve points.",
      notes: "Bring a hopper of balls. Book a hitting partner on the sheet.",
      createdBy: "seed"
    },
    {
      id: "tennis-social",
      sportId: "tennis",
      title: "Friday night mixed doubles",
      type: "SOCIAL_EVENT",
      startsAt: now + days(10),
      endsAt: now + days(10) + hours(3),
      location: "Clubhouse courts",
      description: "Round-robin doubles. Food after the last set.",
      notes: "Non-marking shoes only. Sign the ladder sheet on arrival.",
      createdBy: "seed"
    },
    {
      id: "tennis-resurface",
      sportId: "tennis",
      title: "Court 2 closed for resurfacing",
      type: "ANNOUNCEMENT",
      startsAt: now + days(6),
      location: "Club noticeboard",
      description: "Acrylic top coat needs 48 hours to cure. Bookings move to court 7.",
      notes: "Do not walk on wet coating.",
      createdBy: "seed"
    },
    {
      id: "tennis-footwork",
      sportId: "tennis",
      title: "Split-step and recovery-step ladder",
      type: "PRACTICE",
      startsAt: now + days(13),
      endsAt: now + days(13) + hours(1),
      location: "Court 1 baseline",
      description: "Shadow swings with a ladder, then live feeding to the open court.",
      notes: "Tape ankles if they have rolled this term.",
      createdBy: "seed"
    },
    {
      id: "basketball-mikan",
      sportId: "basketball",
      title: "Mikan and close-range finishing",
      type: "PRACTICE",
      startsAt: now + days(2),
      endsAt: now + days(2) + hours(1.5),
      location: "Indoor court A",
      description: "Both-hand finishes, reverse layups, and contact from a pad.",
      notes: "Indoor shoes. Fill a water bottle — no glass on the timber.",
      createdBy: "seed"
    },
    {
      id: "basketball-derby",
      sportId: "basketball",
      title: "Campus league vs Engineering",
      type: "SOCIAL_EVENT",
      startsAt: now + days(7),
      endsAt: now + days(7) + hours(2),
      location: "Main hall, court 1",
      description: "Four quarters. Students get in with a student card.",
      notes: "Tip-off 19:00. Be taped and warm by 18:20.",
      createdBy: "seed"
    },
    {
      id: "basketball-floor",
      sportId: "basketball",
      title: "Hall floor polish — no training Tuesday",
      type: "ANNOUNCEMENT",
      startsAt: now + days(3),
      location: "Main hall",
      description: "Contractors are resealing the timber. Outdoor courts stay open.",
      notes: "Do not walk on wet sealant.",
      createdBy: "seed"
    },
    {
      id: "basketball-defence",
      sportId: "basketball",
      title: "Shell defence and close-outs",
      type: "PRACTICE",
      startsAt: now + days(14),
      endsAt: now + days(14) + hours(2),
      location: "Outdoor court 2",
      description: "4-on-4 shell, then live close-outs to a contest.",
      notes: "Bring a towel. Surface is abrasive — cover old ankle tape.",
      createdBy: "seed"
    },
    {
      id: "swimming-endurance",
      sportId: "swimming",
      title: "Aerobic 200s on a descending interval",
      type: "PRACTICE",
      startsAt: now + days(1),
      endsAt: now + days(1) + hours(1.5),
      location: "Campus pool, lanes 3–4",
      description: "10 x 200 free, last three at threshold. Slate your splits.",
      notes: "Cap, goggles, and a drinks bottle on the block.",
      createdBy: "seed"
    },
    {
      id: "swimming-gala",
      sportId: "swimming",
      title: "Inter-varsity gala — 100 free heats",
      type: "SOCIAL_EVENT",
      startsAt: now + days(9),
      endsAt: now + days(9) + hours(5),
      location: "Olympic pool, city aquatics centre",
      description: "Heats in the morning, finals in the evening session.",
      notes: "Confirm your heat and lane on the start list the night before.",
      createdBy: "seed"
    },
    {
      id: "swimming-depth",
      sportId: "swimming",
      title: "Shallow end closed for tiling",
      type: "ANNOUNCEMENT",
      startsAt: now + days(4),
      location: "Campus pool",
      description: "No diving until the new tiles are signed off. Lanes 1–2 only.",
      notes: "Enter feet first. Lifeguard on duty from 06:00.",
      createdBy: "seed"
    },
    {
      id: "swimming-turns",
      sportId: "swimming",
      title: "Flip-turn clinic with underwater video",
      type: "PRACTICE",
      startsAt: now + days(12),
      endsAt: now + days(12) + hours(1),
      location: "Campus pool, lane 6",
      description: "Breakout by 15 m. Coach films from underwater on a stick.",
      notes: "Dark cap so the camera can see the head position.",
      createdBy: "seed"
    },
    {
      id: "athletics-blocks",
      sportId: "athletics",
      title: "Block starts and first 30 metres",
      type: "PRACTICE",
      startsAt: now + days(2),
      endsAt: now + days(2) + hours(1.5),
      location: "Home straight, lanes 5–6",
      description: "Reaction practice, then flying 20s from a rolling start.",
      notes: "Spikes on the track only. Walk around the infield.",
      createdBy: "seed"
    },
    {
      id: "athletics-meet",
      sportId: "athletics",
      title: "League meeting — 100 m and long jump",
      type: "SOCIAL_EVENT",
      startsAt: now + days(8),
      endsAt: now + days(8) + hours(4),
      location: "Municipal track",
      description: "Declaration 45 minutes before each event. Hip numbers at the tent.",
      notes: "Bring ID and a spare pair of spikes.",
      createdBy: "seed"
    },
    {
      id: "athletics-sector",
      sportId: "athletics",
      title: "Keep clear of the javelin sector",
      type: "ANNOUNCEMENT",
      startsAt: now + days(5),
      location: "Infield, east sector",
      description: "Throws group is in session until 18:00. Do not cut across.",
      notes: "Implements come from behind you even when the cage looks empty.",
      createdBy: "seed"
    },
    {
      id: "athletics-hurdles",
      sportId: "athletics",
      title: "Lead-leg and trail-leg hurdle work",
      type: "PRACTICE",
      startsAt: now + days(13),
      endsAt: now + days(13) + hours(1.5),
      location: "Back straight",
      description: "Walkovers, then three-step between lowered hurdles.",
      notes: "Tape hamstrings if they twinged last week. No extra 200s after.",
      createdBy: "seed"
    }
  ];
}

module.exports = { seedEvents };
