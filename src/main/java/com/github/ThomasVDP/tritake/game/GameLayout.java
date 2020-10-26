package com.github.ThomasVDP.tritake.game;

import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.Random;

public class GameLayout
{
    int[] board;
    int minimumRow = 0;
    int size;
    boolean isChallengersMove;

    public GameLayout(int size)
    {
        this.size = size;
        board = new int[size];
        for (int i = 1; i <= size; ++i)
        {
            this.board[i - 1] = i;
        }
        Random rand = new Random();
        this.isChallengersMove = rand.nextBoolean();
        System.out.println(this.isChallengersMove);
    }

    public boolean move(int row, int amount)
    {
        if (row > this.size - minimumRow || row < 1) {
            return false;
        }
        if (amount < 1 || amount > this.board[row - 1 + minimumRow]) {
            return false;
        }

        this.board[row -1 + minimumRow] -= amount;
        while (minimumRow < this.size && this.board[minimumRow] == 0) {
            minimumRow++;
        }

        this.isChallengersMove = !this.isChallengersMove;
        return true;
    }

    public String getBoardLayoutForMessage()
    {
        StringBuilder result = new StringBuilder();
        for (int i = minimumRow; i < size; ++i)
        {
            for (int j = 0; j < this.board[i]; ++j)
            {
                result.append(":white_medium_square:");
            }
            result.append('\n');
        }
        if (result.toString().equals("")) {
            return "*empty board*";
        }
        return result.toString();
    }

    public boolean getWhosTurn()
    {
        return this.isChallengersMove;
    }

    public Tuple3<Boolean, Boolean, Boolean> hasWon()
    {
        boolean hasWonOne = Arrays.stream(this.board).filter(o -> o < 2).count() == this.size && Arrays.stream(this.board).filter(o -> o < 1).count() == this.size - 1;
        boolean hasWonTwo = Arrays.stream(this.board).filter(o -> o < 1).count() == this.size;
        return Tuples.of(hasWonOne, hasWonTwo, this.isChallengersMove);
    }
}
