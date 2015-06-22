package com.ludei.multiplayer;


public class MatchRequest
{
    public int minPlayers = 2;
    public int maxPlayers = 2;
    public String[] playersToInvite;
    //A mask that specifies the role that the local player would like to play in the game
    public int playerAttributes;
    //A number identifying a subset of players allowed to join the match.
    public int playerGroup;
}