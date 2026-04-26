# LevelRPG + Enchiridion — Design Philosophy
*Core ideas, implementation principles, and guiding constraints.*
*This document should be consulted before any system is designed or redesigned.*

---

## 1. The Enchiridion's Role

The Enchiridion is not a HUD. It is not a tutorial screen. It is the player's **personal record and the sole interface to the progression system.**

It exists to answer three questions at different points in the player's journey:
1. *"What am I?"* — archetype, name, identity (front cover, character spread)
2. *"How am I growing?"* — passive skill accumulation, discipline progress
3. *"Where do I invest?"* — the skill tree, point spending, unlock states

If a piece of information can go somewhere other than the Enchiridion and still be useful to the player, it probably belongs in the Enchiridion anyway, because the Enchiridion is **where the player is supposed to go to think about this.**

---

## 2. The Unbound Journal Is Intentionally Empty

When a player first receives the Enchiridion, it has **no content.** This is intentional and should not be changed.

- The emptiness is not a bug or a placeholder.
- It is the premise of the discovery loop: the book appears useless until the archetype slot is found.
- Do not add tutorial text, instruction pages, or tooltips inside the unbound book.
- The only exception is the **Bookmark** (see §4).

The tension between "the bookmark just notified me" and "the book appears empty" IS the hook. Preserve it.

---

## 3. The Skill System — Core Model

### 3a. Two-Layer Progression

There are two distinct numbers per skill. They must not be confused or conflated:

| Concept | Meaning | How earned | Suggested name |
|---|---|---|---|
| **Passive accumulation** | How much you have *done* this thing | Automatically, through play | **Practice** (or Aptitude) |
| **Active investment** | How deeply you have *committed* to this discipline | By spending a skill point | **Rank** (or Mastery) |

These two numbers tell a story together. A player with Mining Practice 8 and Mining Rank 2 has done a lot of mining but hasn't invested much into it — there's latent potential. A player with Valor Practice 4 and Valor Rank 4 has converted everything they've earned into deliberate commitment.

The **gap** between these numbers is itself meaningful game information. Do not collapse them into one.

### 3b. The Eight Skills

There are 8 passive skills. Practice in each accumulates through relevant in-game activities:

- **Valor** — combat
- **Mining** — digging, stone, ore
- **Vitality** — survival, eating, taking damage
- **Forging** — crafting, smithing
- **Culinary** — cooking, farming
- **Artificing** — enchanting, advanced crafting
- **Magick** — potion-making, arcane activity
- **Exploration** — traveling, discovering new areas

### 3c. Skill Points

Skill points are the **universal currency** for the skill tree. They are earned passively as Practice levels accumulate across any skill. They are **not locked to a specific skill** — a point earned from Mining can be spent anywhere on the tree.

This is by design. It allows generalists to exist and prevents players from feeling punished for diverse play.

---

## 4. The General Skill Tree — Core Philosophy

### 4a. Structure

The skill tree is a single, connected web with **8 poles** (one per skill) and a shared centre. It is not 8 separate trees — it is one tree with 8 directions.

- **Tier 1 (outer ring):** Always visible. Simple, impactful abilities. Require only a skill point to unlock. No Practice prerequisite. These must feel worth having on their own — not a teaser for the real game.
- **Tier 2–3 (middle):** Require both a skill point AND a minimum Practice level in one or more skills to unlock. Visible but locked until eligible.
- **Cross-nodes:** Sit between two or more poles. Require Practice levels in *multiple* skills. These reward players who have genuinely played across disciplines. They are the most memorable abilities in the system.
- **Deep tier / hidden:** The highest-investment abilities. Some are obfuscated until prerequisites are met. A very small number are completely hidden until revealed.

### 4b. The Three Visibility States

| State | Visible | Requirements shown | Description shown |
|---|---|---|---|
| **Locked** | Yes | Yes | Yes — player knows exactly what they're working toward |
| **Obfuscated** | Yes (silhouette or partial) | Maybe | Hidden — creates curiosity, not confusion |
| **Hidden** | No | No | No — revealed by meeting secret conditions |

**Use sparingly in order:** mostly Locked, some Obfuscated, very few Hidden.

Hidden nodes are the system's long-game hook. When one reveals itself, it should feel like the tree just opened a door the player didn't know was there. If overused, hidden nodes create confusion instead of wonder.

### 4c. Cross-Node Design Principle

Cross-nodes are the soul of the system. They must feel like the game is *acknowledging a real pattern of play*, not just demanding an arbitrary number combination.

> A Mining + Exploration cross-node should feel like: "You've been underground a lot AND you've traveled far. You understand the deep world."

The ability granted by a cross-node should thematically match the combination. If it doesn't, redesign the ability, not the prerequisites.

### 4d. The Archetype Layer

The archetype chosen at binding affects the skill tree. Exact implementation is TBD, but the principle is:

- An archetype should **reduce the cost or threshold** for nodes near its affiliated pole(s), not add exclusive locked-off branches.
- Some hidden nodes may only **reveal themselves** based on the chosen archetype — meaning two players can look at the same tree and see different things. This is a strong multiplayer hook and should be preserved.
- The archetype is a *lens*, not a wall.

### 4e. What the Tree Must Never Be

- ❌ A wall of identical passive bonuses (+5% damage, +5% damage, +5% damage)
- ❌ A system where the only real choices are in the last tier
- ❌ A system where specialisation is punished (cross-nodes should *reward* diverse play, not require it for core progress)
- ❌ A single-page screen dump where everything is visible at once

---

## 5. The Bookmark — Notification System

### 5a. Purpose

The Bookmark is a **diegetic notification system.** It is a physical object in the world of the book. It does not editorialize, give advice, or comment on player behaviour. Its only role is to tell the player when something in the book has changed.

### 5b. Behaviour

**When the player is outside the book:**
- Sends a short chat message in **red text** (the bookmark's "voice")
- Example messages:
  - `"The Enchiridion shakes."`
  - `"Something stirs within the Enchiridion."`
  - `"A new mark has appeared in the Enchiridion."`
- Messages are short, impersonal, in-world. No exclamation points. No explanations.

**When the player is inside the book:**
- Does NOT send chat messages
- Uses a **chat bubble or small UI element** attached to the bookmark visual
- May optionally auto-navigate to the first new entry, then stop and return control to the player

**Tour behaviour (when multiple things changed):**
- On the first open after a notification, the bookmark highlights pages with new content
- It navigates to each one in sequence, briefly, then returns the player to where they were
- This happens **once per notification batch**, not on subsequent opens
- It is interruptible — the player can skip it at any point

### 5c. Triggers

The bookmark fires a notification when:
- A skill point becomes available (Practice crossed a threshold)
- A new skill tree node becomes visible (locked → visible, or hidden → revealed)
- The book is successfully bound for the first time (archetype sealed)
- A milestone is crossed within any skill

The bookmark does **not** fire for:
- XP gain
- Stat changes that don't cross a meaningful threshold
- Any event that doesn't produce something actionable or viewable in the book

### 5d. Tone Constraint

> The bookmark is a messenger, not a companion. It has no personality beyond quiet urgency. It should never feel like a tutorial pop-up.

If a proposed notification message sounds like it could come from a tutorial system, cut it. If it sounds like something that belongs in an old parchment, keep it.

---

## 6. The First-Play Loop

This is the intended player experience on first encounter. Do not break any step of this:

1. Player receives the Enchiridion
2. `"The Enchiridion shakes."` appears in chat (red) — *the bookmark's first message*
3. Player opens the book — **empty pages**
4. Player is confused. The book appears useless.
5. Player discovers the archetype card slot (front cover recess)
6. Player engages with the reel, reads archetype descriptions, makes a choice
7. Archetype is bound — the cover title appears, the interior pages fill in
8. Player opens the book again — now it has content, identity, and a visible Tier 1 skill tree

The gap between steps 2 and 4 — notification with no visible payoff — is intentional. It creates the question that step 5 answers. **Do not short-circuit this by adding explanation text to the empty pages.**

---

## 7. Immersion and Tone

- All text in the book is written **as if the book is a real object in the world**, not as a game UI
- Raw numbers (total XP, next threshold XP) that have no in-world meaning should be replaced or removed
- Skill levels are referred to by in-world names, not "Skill Level 3" or "Level Up"
- The archetype is referred to as a *path*, *mark*, or *seal* — never a "class" or "character type"
- The bookmark speaks in short, old-world declarative sentences. No contractions. No emoji.

---

## 8. "Starting Weaker, Growing Stronger"

The mod is an **enhancement to vanilla progression**, not a replacement. The design goal:

- Players start slightly weaker than unmodded vanilla
- As disciplines develop, they grow meaningfully stronger — not just numerically, but in kind (new abilities, not just bigger numbers)
- Each player's build should reflect their actual play history
- Specialisation should feel special — other players should be able to look at a build and read a story from it
- Players should feel a reason to keep playing, keep grinding, and keep returning to the Enchiridion to process what they've earned

The book is the **reason the grind has meaning.** Without it, the numbers are just numbers.

---

## 9. The Radar Chart — Stat Visualisation

### 9a. Purpose

The radar chart (octagonal spider chart) replaces the raw numeric stat display. Its purpose is to communicate the **shape of a build at a glance**, not a list of numbers. A combat-focused player has a chart that looks combat-focused. A generalist has a round, even shape. The shape IS the identity.

### 9b. Structure

- Eight axes, one per skill, radiating from a shared centre
- **Outer polygon (Practice):** How far each skill has been passively accumulated through play
- **Inner polygon or markers (Rank):** How much deliberate investment has been placed in each skill
- The gap between the two polygons on any axis = unclaimed potential in that discipline

Both polygons are shown simultaneously. A third visual element — a ring, dash, or tick mark on each axis — indicates the current **cap position** (Practice Rank + 1 gap limit) so the player always knows how close they are to capping a skill without opening any submenu.

### 9c. Interaction

- Hovering near a vertex shows a tooltip: skill name, Practice level, Rank level, and whether a skill point is ready
- Hit regions are small rectangles placed over each vertex — not polygon-based hit testing
- The chart itself is rendered into the page texture via `Graphics2D` (filled polygon + axis lines)
- No animation needed; re-renders when state changes

### 9d. Placement

The chart replaces the Character Record left page (**Option A**). It is the first thing the player sees when they open a bound book. The right page of the same spread (currently "Standing") remains for any supporting text or summary.

### 9e. What the Chart Must Communicate Without Words

A player should be able to open the book, glance at the chart for two seconds, and know:
- Which skills are their strongest (longest axes)
- Whether their investments are keeping up with their practice (inner vs. outer polygon gap)
- If any skill has hit its cap (Practice polygon pressing against the Rank ring boundary)

---

## 10. The Skill Cap System

### 10a. Principle

Each skill's Practice level cannot advance indefinitely without investment. Once Practice reaches a threshold above the player's current Rank in that skill, it stops growing (or slows dramatically). This prevents every player from reaching the same passive high-water mark regardless of choices made.

The cap exists to create **decision pressure**, not to punish the player. It must always be communicated clearly — via the radar chart's visual, and via a bookmark notification before the cap is hit.

### 10b. The Gap

The allowed gap is **Rank + 1**. A skill's Practice level cannot exceed the player's current Rank in that skill by more than one level.

> ⚠️ Known concern: a fixed +1 offset feels somewhat arbitrary and may not scale well as max levels are established. This value should be revisited once the maximum Practice/Rank levels are defined. The underlying principle (a fixed lead before pressure kicks in) is decided; the exact number is subject to tuning.

### 10c. Hard Stop vs. Soft Slowdown

> ⚠️ TBD: Hard stop or steep diminishing return. Decision deferred to playtesting. The gap value (§10b) and the stop type interact — a Rank+1 hard stop is tight and demanding; a Rank+1 soft stop is more forgiving. Resolve together once max levels are established.

### 10d. Visual Signal

The radar chart is the primary signal for a capped skill. When Practice is pressing against its Rank limit, the outer polygon is visibly crowded against the inner marker on that axis. The player can see the cap before they hit it if they check the chart.

The bookmark fires `"[Skill] practice nears its limit."` one Practice level before the cap is reached — giving the player one session's warning.

### 10e. What the Cap Must Never Feel Like

- ❌ A bug (requires clear visual communication — see §10d)
- ❌ A punishment for playing naturally (it is a decision prompt, not a penalty)
- ❌ A surprise (the bookmark and chart exist specifically to prevent this)

---

## 11. The Point System

### 11a. Source

Skill points are earned through **Practice accumulation across any skill.** Each time Practice advances in any discipline, it contributes toward a shared point counter. When the counter fills, one skill point is awarded.

Points are **not tied to the skill that generated them.** A point earned from Mining can be spent anywhere on the tree.

### 11b. Point Rate

**1 Practice level earned = 1 skill point.** The conversion is direct and linear.

This rate is intentionally simple for the current design phase. Once maximum skill levels are established, this may increase — a rate of **3 points per Practice level** has been considered, which would give players more freedom and experimentation room per level gained.

> ⚠️ Revisit when max levels are set: the 1:1 rate may be too slow at higher levels, and the 3:1 rate may be too generous at low levels. A tiered rate (1 point at low Practice, 2–3 at higher Practice) is worth considering as an alternative.

Activity weighting (where rarer skills contribute more per level to compensate for lower activity frequency) is deferred until real activity data from playtesting is available. The base 1:1 rate applies uniformly for now.

### 11c. Points Are Permanent

Once earned, skill points exist until spent. Once spent, they are gone permanently — with the exceptions described in §12 (Respec).

There is no decay, expiry, or conversion. Hoarding points is allowed but discouraged by the skill cap system (§10) — a capped skill creates pressure to spend.

### 11d. The Bookmark's Role

When a point becomes available, the bookmark fires: `"The Enchiridion shakes."` The player is expected to open the book and spend it, or consciously choose to hold it. The book is the only place to spend points. There is no shortcut spending menu.

---

## 12. The Respec System

### 12a. Points Are Permanent By Default

Spending a point is a commitment. Early decisions carry weight. This is intentional — the first playthrough is a learning run, and the respec is the reward for completing it.

### 12b. Tier 1 Refund Window

**Before any Tier 2 point is spent**, Tier 1 abilities can be freely refunded and reallocated. This is the only automatic flexibility in the system.

Once the player spends their first Tier 2 point, the refund window closes permanently (until a full respec is earned). This gives new players a small safety net without trivialising the full commitment.

### 12c. The Prestige Respec

A full respec is earned — not purchased — by reaching a significant investment threshold on the skill tree (a "Tier 3 complete" or equivalent milestone). This happens **at most twice per player**, with the second requiring a higher threshold than the first.

What resets on a full respec:
- All Rank levels are returned as unspent points
- The inner polygon (Rank) on the radar chart resets
- The archetype may optionally be re-chosen (TBD — depends on whether the archetype is considered part of the identity or part of the build)

What does **not** reset:
- Practice levels — the player's history of activity is permanent
- The outer polygon (Practice) on the radar chart remains
- The player's name on the cover remains

> The lore principle: *"The disciplines can be unlearned. The experience cannot."*

### 12d. The Respec Structure

The respec is performed at a **physical world structure** — a specific block or multiblock placed in the world. It is not a menu option, not a command, not a UI button.

Design principles for the structure:
- **Portable:** Any server owner or map maker can place it anywhere. Its location is not hardcoded.
- **Weighted:** The activation should require a physical action beyond just right-clicking — something placed, dropped, or offered to the structure. The player should be able to pause after initiating and walk away before confirming.
- **Gated:** The structure does nothing for a player who has not yet earned a respec. Interacting with it before that threshold is reached produces no effect (or a brief acknowledgment that it isn't time yet).
- **Ritualistic:** The experience of using it should feel different from any other interaction in the game. It is a threshold moment, not a menu.

### 12e. What the Respec Must Never Be

- ❌ Farmable (it is earned through build completion, not resource accumulation)
- ❌ Available on demand (scarcity is what makes each run meaningful)
- ❌ A full erase (Practice is permanent — the player's history survives)
- ❌ A menu confirmation dialog (it is a physical ritual in the world)

---

## 13. The Living Book — Unlocking Content Over Time

### 13a. Principle

The Enchiridion is not static. It **grows with the player.** Each significant threshold in the progression system should cause something in the book to visibly change — new content appears, existing content fills in, the physical object itself looks different. There is no "book level" number. The book's evolution *is* the level.

The player should be able to open the book after a long session and notice that it looks different from when they last closed it. That moment of noticing — "something changed" — is the emotional payoff that creates return visits.

### 13b. Threshold Triggers and What They Unlock

The following thresholds each cause a visible change to the book:

| Threshold | What changes |
|---|---|
| Enchiridion received (unbound) | Book exists but is empty — the cover title is blank, pages have no content |
| Archetype bound | Cover title fills in (`"[Name]'s Legacy"`), the interior pages materialise, Tier 1 skill tree becomes visible |
| First skill point spent | The relevant discipline page gains its first entry — the player's commitment is acknowledged |
| Cross-node unlocked | A new page or section appears in the book describing the cross-discipline ability — it wasn't there before |
| Hidden node revealed | A previously blank page gains ink — the description of the hidden ability appears as if written by an unseen hand |
| Milestone crossed within a skill | A new line or entry appears on that skill's page — a record of the milestone, dated in-world |
| Prestige earned | Something changes about the cover or the bookmark — a new mark, a different material, a Roman numeral |

This list is not exhaustive. Any meaningful threshold that a player would want to remember belongs here.

### 13c. Blank Pages Are Intentional

Some pages in the book exist from the moment of binding but contain no text. They are not bugs or placeholders. The player who opens the book early and finds a blank page should wonder what goes there — and eventually find out.

Do not fill blank pages preemptively with placeholder text. The blankness is part of the design.

### 13d. The Prestige Volume Mark

After a prestige respec is completed, a **Roman numeral** appears on the spine or cover of the book — Volume I after the first prestige, Volume II after the second. This is the only visible "count" on the book's exterior.

A Volume II Enchiridion seen in another player's hand communicates immediately that they have built and rebuilt their disciplines once already. Volume III would be extraordinarily rare. No number is needed — the volume mark carries the full meaning.

### 13e. The Cover as a Record

Over time, the book's cover should accumulate marks that reflect the player's history:
- The archetype seal (inscribed at binding)
- The legacy title (player's name)
- The volume numeral (after prestige)
- Optionally: additional marks for major milestones (first cross-node, full Tier 1 completion, etc.)

The cover is the player's public-facing identity. Another player who sees the cover should be able to read something meaningful from it — not stats, but *history.*

### 13f. What the Living Book Must Never Become

- ❌ A checklist of tasks to complete (the changes should feel like discoveries, not objectives)
- ❌ Overwhelming — changes should be spaced out and meaningful, not constant
- ❌ Retroactive — if a page unlocks at a threshold, it should not silently appear during a session without the bookmark notifying the player
- ❌ Reversible on prestige — the historical record (milestone entries, cover marks) survives a respec; only the Rank investments reset

---

*Last updated: April 2026*
