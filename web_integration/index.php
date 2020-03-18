<?php
/**
 * index.php
 *
 * OptEcoWebAPI Project
 *
 * Please read this requirement before use
 * - Compatible with PHP 7 (didn't test on old versions)
 * - Requires the mysqli library: https://www.php.net/manual/en/book.mysqli.php
 *
 * With configuration, you can check the APIConfig.php.
 */

require_once "api/OptEcoAPI.php";
require_once "api/PlayerAPI.php";

$api = new OptEcoAPI(new OptEcoMySQL());

var_dump($api->createAccount("IronMan"));

