# 🐛 MSNReports - Advanced Bug Reporting System

Hey there! 👋 Welcome to MSNReports, a powerful and user-friendly bug reporting plugin for Paper/Spigot servers. This plugin makes it super easy for your players to report bugs, and for staff to manage those reports effectively.

## ✨ Features

- 🎮 **Interactive Chat Interface** - Beautiful, clickable messages for easy report management
- 🔒 **Secure Data Storage** - All reports are safely stored in SQLite database
- 🎯 **Discord Integration** - Instant notifications through Discord webhooks
- 📊 **Report Status System** - Track bug reports from submission to resolution
- 🛡️ **Permission-Based Access** - Granular control over who can do what
- 🎨 **Clean UI** - User-friendly interface for both players and staff

## 📋 Commands

### For Players
- `/reportbug` - Submit a new bug report
  - Permission: `msnreports.report`

### For Staff
- `/managereports` - Main command for managing reports
  - `/managereports list [page]` - View all reports
  - `/managereports view <id>` - View detailed report info
  - `/managereports status <id> <status>` - Update report status
  - Permission: `msnreports.manage`

## 🚀 Installation

1. Download the latest release from [GitHub Releases](https://github.com/msncakma/msnReports/releases)
2. Place the .jar file in your server's `plugins` folder
3. Restart your server
4. Configure the Discord webhook URL in `config.yml`

## ⚙️ Configuration

The plugin is super easy to configure! Just edit the `config.yml` file:

\`\`\`yaml
# Your license key (required)
license: 'github.com/msncakma'

# Discord settings
discord:
  webhook-url: 'YOUR_WEBHOOK_URL_HERE'
\`\`\`

## 🔑 Permissions

- `msnreports.report` - Allow players to report bugs
- `msnreports.manage` - Access to report management
- `msnreports.manage.view` - Allow viewing report details
- `msnreports.manage.status` - Allow changing report status

## 🤝 Contributing

Hey! Feel like making this plugin even better? Awesome! Here's how you can help:

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is under the MIT License - see the [LICENSE](LICENSE) file for details.

## 💖 Support

If you found this plugin helpful, don't forget to give it a star ⭐ on GitHub! Got questions? Feel free to:

- Open an [issue](https://github.com/msncakma/msnReports/issues)
- Contact me on Discord: msncakma
- Visit our [Discord server](https://discord.gg/YOUR_SERVER) (Coming soon!)

## 🙏 Credits

Made with ❤️ by [msncakma](https://github.com/msncakma)

---
*Stay awesome and keep your server bug-free! 🚀*