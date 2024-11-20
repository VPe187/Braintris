# Szakdolgozat: Tetris-t játszó neurális hálózat

### Nyíregyházi Egyetem

#### Készítette: Varga Péter
#### Témavezető: Dr. Vegera József

---

## Tartalomjegyzék

1. [Bevezetés](#bevezetés)
2. [Háttér és motiváció](#háttér-és-motiváció)
3. [Neurális hálózatok és Tetris](#neurális-hálózatok-és-tetris)
4. [Q-learning alkalmazása](#q-learning-alkalmazása)
5. [Modell felépítése és tréningje](#modell-felépítése-és-tréningje)
6. [Eredmények](#eredmények)
7. [Összegzés](#összegzés)
8. [Hivatkozások](#hivatkozások)

---

## Bevezetés

A Tetris játék az egyik legismertebb és legnépszerűbb logikai játék, amely egyszerű szabályai ellenére komoly kihívást jelent a mesterséges intelligencia számára. E szakdolgozat célja egy olyan neurális hálózat kidolgozása, amely képes hatékonyan játszani a Tetris játékot, Q-learning algoritmus segítségével.

## Háttér és motiváció

A Tetris tökéletes környezet a megerősítéses tanulás vizsgálatára, mivel a játék folyamatos visszajelzést ad a játékos számára, és a cél egy minél jobb eredmény elérése. A neurális hálózatok és a megerősítéses tanulás kombinálása lehetőséget ad arra, hogy egy gépi tanulási algoritmus fejlessze magát és optimalizálja a játékstratégiát.

## Neurális hálózatok és Tetris

A neurális hálózatok olyan gépi tanulási modellek, amelyek képesek komplex mintázatok felismerésére és megtanulására. A Tetris játék esetén a hálózatnak meg kell tanulnia, hogyan helyezze el a különböző formájú elemeket a játékmezőn úgy, hogy minél több sort tudjon eltüntetni, minimalizálva a lyukak számát.

## Q-learning alkalmazása

A Q-learning egy gyakran használt algoritmus a megerősítéses tanulásban, amely arra szolgál, hogy megtanítsa a hálózatot, hogyan válassza ki a legjobb akciót az adott állapotban. Ebben a projektben a Q-learninget használjuk a Tetris játék optimalizálására, ahol az állapotokat és az akciókat a játékmező és az aktuális elem paraméterei határozzák meg.

## Modell felépítése és tréningje

A fejlesztett neurális hálózat három rejtett rétegből áll, mindegyik 64 neuronból. A bemeneti rétegben hat különböző jellemző található, amelyek a játéktábla és az elemek állapotát írják le. A kimeneti réteg egyetlen Q-értéket számít ki, amely az adott akció értékét képviseli. A modell tréningje során Q-learninget és tapasztalati ismétlést alkalmazunk, hogy javítsuk a tanulási hatékonyságot.

## Eredmények

A modell több ezer játék után képes volt felismerni az optimális elhelyezési stratégiákat, és jelentős javulást mutatott az eredményekben. Az átlagos pontszám és az eltávolított sorok száma is folyamatosan növekedett a tanulás során, azonban bizonyos kihívások és ciklikus teljesítményingadozások megfigyelhetőek voltak.

## Összegzés

A szakdolgozat során bemutattuk, hogyan lehet egy neurális hálózatot megtanítani a Tetris játékra Q-learning segítségével. Az eredmények azt mutatják, hogy a modell képes tanulni és javítani a játékstratégiáját, de további fejlesztésekre van szükség a teljes optimalizáció érdekében.

## Hivatkozások

1. Sutton, R. S., & Barto, A. G. (2018). **Reinforcement Learning: An Introduction**. MIT Press.
2. Mnih, V., Kavukcuoglu, K., Silver, D., et al. (2015). **Human-level control through deep reinforcement learning**. Nature, 518(7540), 529-533.
3. Tesauro, G. (1995). **Temporal difference learning and TD-Gammon**. Communications of the ACM, 38(3), 58-68.

---

*További információkért, kérlek keresd meg a témavezetőt vagy látogass el a [Nyíregyházi Egyetem weboldalára](https://www.nye.hu/).*
