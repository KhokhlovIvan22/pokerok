import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class HandEvaluator {
    private static List<Card> pick(List<Card> cards, int rank, int count, List<Card> avoid) {
        List<Card> found = new ArrayList<>();
        int target = (rank == 1) ? 14 : rank;
        for (Card c : cards) {
            if (found.size() < count && c.getPower().getRank() == target && !avoid.contains(c)) {
                found.add(c);
            }
        }
        return found;
    }

    private static int getStraightHighest (int bitmask) {
        for (int i=14; i>=5; --i)
            if ((bitmask & (0x1F << (i-4))) == (0x1F << (i-4)))
                return i;
        if ((bitmask & 0x403C) == 0x403C)
            return 5;
        return 0;
    }

    private static List<Integer> getKickers (int[] counts, int ex1, int ex2, int need) {
        List<Integer> k = new ArrayList<>();
        for (int r=14; r>=2; --r) {
            if (r == ex1 || r == ex2)
                continue;
            for (int i=0; i < counts[r] && k.size() < need; ++i)
                k.add(r);
        }
        return k;
    }

    private static long score (int category, int... r) {
        long s = category * (long)Math.pow(15, 5);
        for (int i = 0; i < r.length; ++i)
            s += r[i] * (long)Math.pow(15, 4-i);
        return s;
    }

    public static HandResult evaluate (List<Card> originalCards) {
        List<Card> cards = new ArrayList<>(originalCards);
        cards.sort((c1,c2)->Integer.compare(c2.getPower().getRank(), c1.getPower().getRank()));
        int[] rankCount = new int[15];
        int[] suitCount = new int[4];
        int bitmask = 0;
        for (Card c: cards) {
            suitCount[c.getSuit().ordinal()]++;
            rankCount[c.getPower().getRank()]++;
            bitmask |= (1<<c.getPower().getRank());
        }

        for (int s=0; s<4; ++s) {
            if (suitCount[s] >= 5) {
                final int suitIdx = s;
                List<Card> flushCards = cards.stream()
                        .filter(c -> c.getSuit().ordinal() == suitIdx)
                        .collect(Collectors.toList());

                int fMask = 0;
                for (Card c : flushCards)
                    fMask |= (1 << c.getPower().getRank());

                int sf = getStraightHighest(fMask);
                if (sf > 0) {
                    List<Card> best = new ArrayList<>();
                    for (int r = sf; r > sf - 5; r--)
                        best.addAll(pick(flushCards, r, 1, best));
                    HandRank hr = (sf == 14) ? HandRank.ROYAL_FLUSH : HandRank.STRAIGHT_FLUSH;
                    return new HandResult(hr, score(hr == HandRank.ROYAL_FLUSH ? 10 : 9, sf), hr.name(), best);
                }

                List<Card> best = flushCards.subList(0, 5);
                return new HandResult(HandRank.FLUSH, score(6, best.getFirst().getPower().getRank()), "Флеш", best);
            }
        }

        int quad=0, trip=0;
        List<Integer> pairs = new ArrayList<>();
        for (int r=14; r>=2; --r) {
            if (rankCount[r] == 4)
                quad = r;
            else if (rankCount[r] == 3 && trip == 0)
                trip = r;
            else if (rankCount[r] >= 2)
                pairs.add(r);
        }

        if (quad > 0) {
            List<Card> best = pick(cards, quad, 4, new ArrayList<>());
            List<Integer> k = getKickers(rankCount, quad, -1, 1);
            best.addAll(pick(cards, k.getFirst(), 1, best));
            return new HandResult(HandRank.FOUR_OF_A_KIND, score(8, quad, k.getFirst()), "Каре", best);
        }

        if (trip > 0 && !pairs.isEmpty()) {
            List<Card> best = pick(cards, trip, 3, new ArrayList<>());
            best.addAll(pick(cards, pairs.getFirst(), 2, best));
            return new HandResult(HandRank.FULL_HOUSE, score(7, trip, pairs.getFirst()), "Фулл-хаус", best);
        }

        int st = getStraightHighest(bitmask);
        if (st > 0) {
            List<Card> best = new ArrayList<>();
            for (int r = st; r > st - 5; r--)
                best.addAll(pick(cards, r, 1, best));
            return new HandResult(HandRank.STRAIGHT, score(4, st), "Стрит", best);
        }

        if (trip > 0) {
            List<Card> best = pick(cards, trip, 3, new ArrayList<>());
            List<Integer> k = getKickers(rankCount, trip, -1, 2);
            for (int r : k)
                best.addAll(pick(cards, r, 1, best));
            return new HandResult(HandRank.THREE_OF_A_KIND, score(3, trip, k.get(0), k.get(1)), "Сет", best);
        }

        if (pairs.size() >= 2) {
            int p1 = pairs.get(0), p2 = pairs.get(1);
            List<Card> best = pick(cards, p1, 2, new ArrayList<>());
            best.addAll(pick(cards, p2, 2, best));
            int k = getKickers(rankCount, p1, p2, 1).getFirst();
            best.addAll(pick(cards, k, 1, best));
            return new HandResult(HandRank.TWO_PAIRS, score(2, p1, p2, k), "Две пары", best);
        }

        if (pairs.size() == 1) {
            int p = pairs.getFirst();
            List<Card> best = pick(cards, p, 2, new ArrayList<>());
            List<Integer> k = getKickers(rankCount, p, -1, 3);
            for (int r : k)
                best.addAll(pick(cards, r, 1, best));
            return new HandResult(HandRank.PAIR, score(1, p, k.get(0), k.get(1), k.get(2)), "Пара", best);
        }

        List<Card> best = cards.subList(0, 5);
        List<Integer> k = getKickers(rankCount, -1, -1, 5);
        return new HandResult(HandRank.HIGH_CARD, score(0, k.get(0), k.get(1), k.get(2), k.get(3), k.get(4)), "Высшая карта", best);
    }
}