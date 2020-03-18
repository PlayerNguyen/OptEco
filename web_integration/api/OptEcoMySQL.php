<?php
/**
 * OptEcoMySQL.php
 *
 * OptEcoWebAPI Project
 *
 * Please read this requirement before use
 * - Compatible with PHP 7 (didn't test on old versions)
 * - Requires the mysqli library: https://www.php.net/manual/en/book.mysqli.php
 *
 * With configuration, you can check the APIConfig.php.
 *
 */

require_once "api/APIConfig.php";
class OptEcoMySQL
{

    private $mysqli;

    public function __construct()
    {
        /*
         * Check whether the extension is enabled
         */
        if (!extension_loaded("mysqli")) {
            die("Requires mysqli library, please enable it!");
        }
        /*
         * Connect to the mysqli
         */
        $this->mysqli = new mysqli(
            Config::$MYSQL['host'],
            Config::$MYSQL['username'],
            Config::$MYSQL['password'],
            Config::$MYSQL['database'],
            Config::$MYSQL['port']
        );
    }

    /**
     * Get the connection
     * @return mysqli
     */
    public function getMySQLi()
    {
        return $this->mysqli;
    }

    /**
     * Check whether system are connect or not.
     * @since 1.0
     */
    public function isConnect() {
        return $this->mysqli->errno == 0;
    }

}