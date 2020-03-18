<?php
/**
 * example_top.php
 *
 * Code in OptEcoWebAPI Project
 */

const DISPLAY_MONEY = true;

/**
 * Require Libraries
 */
require_once "api/OptEcoAPI.php";
require_once "api/PlayerAPI.php";
?>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="description" content="">
    <meta name="keywords" content="">
    <meta name="author" content="">
    <meta name="theme-color" content="">
    <meta name="viewport"
          content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Top Riches Player</title>

    <link rel="stylesheet" href="css/stylesheet.css">

</head>
<body>

    <div class="header">
        <h1 class="header-title">< Top riches ></h1>
        <h3 class="header-description">Top riches people in server</h3>
    </div>

    <div class="container">
        <h2 id="top5"><a href="#top5"># Top 5</a></h2>
        <table class="top-player-donate w-100">
            <?php
            $api = new OptEcoAPI(new OptEcoMySQL());
            $players = $api->getAccounts("`player`, `balance`", "ORDER BY `balance` DESC LIMIT 5");
            ?>
            <tr>
                <th>#</th>
                <th>Avatar</th>
                <th>Player</th>
                <?php if (DISPLAY_MONEY) echo "<th>Money</th>"; ?>
            </tr>
            <?php
            if (sizeof($players) < 1) {
                echo "<tr><td></td><td></td><td><i>Nothing in this list</i></td><td></td></tr>";
            } else {
                $counter = 0;
                foreach ($players as $player) {
                    $counter ++;
                    echo "<tr>";
                    echo "<td class='center'>{$counter}</td>";
                    echo "<td class='center'><img src='https://minotar.net/avatar/{$player['player']}/20' alt='face'></td>";
                    echo "<td>{$player['player']}</td>";
                    if (DISPLAY_MONEY) echo "<td class='center'>{$player['balance']}</td>";
                    echo "</tr>";
                }
            } ?>
        </table>
    </div>

</body>
</html>
