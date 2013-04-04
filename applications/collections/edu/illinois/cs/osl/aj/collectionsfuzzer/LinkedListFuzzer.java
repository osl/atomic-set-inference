package edu.illinois.cs.osl.aj.collectionsfuzzer;

import java.util.Random;

import collections.Arrays;
import collections.List;

public class LinkedListFuzzer
{

    private static List<Object> sharedList1;
    private static List<Object> sharedList2;

    private static class FuzzerThread extends Thread
    {

        private Random randomizer;

        public FuzzerThread(long seed)
        {
            randomizer = new Random(seed);
        }

        @Override
        public void run()
        {
            for (int i = 0; i < 500; ++i)
            {
                System.out.print(".");
                List<Object> list = randomizer.nextBoolean() ? sharedList1 : sharedList2;

                int operation = randomizer.nextInt(9);

                try
                {
                    switch (operation)
                    {
                        case 0:
                            list.add(randomizer.nextInt(list.size()), new Object());
                            break;
                        case 1:
                            if (randomizer.nextBoolean())
                                list.addAll(list == sharedList1 ? sharedList2 : sharedList1);
                            else
                            {
                                Object[] a = new Object[]
                                {
                                    new Object(), new Object(), new Object()
                                };
                                list.addAll(Arrays.asList(a));
                            }
                            break;
                        case 2:
                            list.removeAll(list == sharedList1 ? sharedList2 : sharedList1);
                            break;
                        case 3:
                            list.clear();
                            break;
                        case 4:
                            list.contains(new Object());
                            break;
                        case 5:
                            list.get(randomizer.nextInt(list.size()));
                            break;
                        case 6:
                            list.remove(randomizer.nextInt(list.size()));
                            break;
                        case 7:
                            list.set(randomizer.nextInt(list.size() - 1), new Object());
                            break;
                        case 8:
                            list.toArray();
                            break;
                    }

                    sleep(10);
                } catch (Exception e)
                {
                    // Silently ignore exceptions.
                    System.out.print("!");
                }
            }
        }
    }
}
