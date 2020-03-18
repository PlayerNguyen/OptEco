<?php
/**
 * OptEcoAPI.php
 * OptEcoWebAPI Project
 *
 * Please read this requirement before use
 * - Compatible with PHP 7 (didn't test on old versions)
 * - Requires the mysqli library: https://www.php.net/manual/en/book.mysqli.php
 *
 * With configuration, you can check the APIConfig.php.
 *
 * !! This API using server-side process, that mean the MySQL server will process all data;
 * If you want to use client-side (PHP) please check ours another API;
 *
 */

require_once "OptEcoMySQL.php";
class OptEcoAPI
{

    private $connect;
    private $table;

    /**
     * OptEcoAPI constructor.
     * @param $connect OptEcoMySQL
     */
    public function __construct($connect)
    {
        $this->connect = $connect;
        $this->table = Config::$MYSQL['table'];
    }

    /**
     * Get the connection
     * @return OptEcoMySQL
     */
    public function getConnect(): OptEcoMySQL
    {
        return $this->connect;
    }

    /**
     * Get the table to execute
     * @return mixed
     */
    public function getTable()
    {
        return $this->table;
    }

    /**
     * Create new account, using for register
     * Attention to the ONLINE_MODE if you having a offline server
     *
     * @param $player
     * @return bool|mysqli_result
     * @since 1.0
     */
    public function createAccount($player) {
        /**
         * Process input
         */
        $player = addslashes($player);
        $balance = "0.0";
        $playerAPI = new PlayerAPI($player);

        return $this->exec(
            sprintf(
                /** @lang text */ "INSERT INTO `%s` (`player`, `balance`, `uuid`) VALUES ('%s', '%s', '%s')",
                $this->getTable(),
                $player,
                $balance,
                $playerAPI->getUUID()
            )
        );
    }

    /**
     * Get an account of player.
     * Return the array like [player, balance].
     *
     * @param $player
     * @return mixed|null
     * @since 1.0
     */
    public function getAccount($player) {
        $player = addslashes($player);
        $exec = $this->get("`player`, `balance`", "WHERE `player`='{$player}'");
        if (!$exec) return null;
        return $exec->fetch_array(MYSQLI_ASSOC);
    }

    /**
     * Get all data account with condition
     * Caution: This method will leak all of your data, be careful!
     *
     * @param $what
     * @param string $condition
     * @return mixed
     * @since 1.0
     */
    public function getAccounts($what, $condition = "") {
        $exec = $this->get($what, $condition);
        return $exec->fetch_all(MYSQLI_ASSOC);
    }

    /**
     * Check whether has player or not
     * @param $player string player name
     * @return bool
     * @since 1.0
     */
    public function hasAccount($player) {
        return $this->getAccount($player) != null;
    }

    /**
     * Update the balance of player
     *
     * @param $player
     * @param $balance
     * @return bool|mysqli_result
     * @since 1.0
     */
    public function updateBalance($player, $balance) {

        $player = addslashes($player);
        $balance = addslashes($balance);
        return $this->exec(
            sprintf(/** @lang text */ "UPDATE `%s` SET `balance`='%s' WHERE `player`='%s'", $this->getTable(), $balance, $player)
        );
    }

    /**
     * Get balance of player, return as float value
     *
     * @param $player
     * @return float
     * @since 1.0
     */
    public function getBalance($player) {
        return doubleval($this->getAccount($player)['balance']);
    }

    public function addBalance($player, $adder) {
        $currentBalance = doubleval($this->getBalance($player));
        $newBalance = doubleval($currentBalance + $adder);
        return $this->updateBalance($player, $newBalance);
    }

    /**
     * Execute the query
     * @param $query
     * @return bool|mysqli_result
     * @since 1.0
     */
    protected function exec($query) {
        $exec = $this->getConnect()->getMySQLi()->query($query);
        if ($exec == false) die(sprintf("Error while calling execute => [%s]...",
            $this->getConnect()->getMySQLi()->error));
        return $exec;
    }

    /**
     * Get account from table
     * @param $data
     * @param $conditions
     * @return bool|mysqli_result
     * @since 1.0
     */
    protected function get($data, $conditions) {
        return $this->exec(
            sprintf(
                /** @lang text */ "SELECT %s FROM `%s` %s",
                $data,
                $this->getTable(),
                $conditions)
        );
    }

}