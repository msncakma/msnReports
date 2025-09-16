# 🐛 MSNReports v1.3-BETA - Advanced Bug Reporting System

Hey there! 👋 Welcome to MSNReports, a powerful and user-friendly bug reporting plugin for Paper/Spigot servers. This plugin makes it super easy for your players to report bugs, and for staff to manage those reports effectively.

## ✨ Features

- 🎮 **Beautiful GUI Confirmation** - Interactive inventory-based confirmation system
- 🎯 **Multiple Discord Webhooks** - Separate channels for reports, status changes, admin notes
- 🗄️ **Database Choice** - Support for both SQLite and MySQL databases
- 🔒 **Secure Data Storage** - AES encryption for sensitive information
- 📊 **Advanced Report Management** - Full lifecycle tracking and status system
- 🛡️ **Permission-Based Access** - Granular control with detailed permissions
- � **Clean Discord Integration** - Rich embeds with automatic color code removal
- ⚡ **Thread Safety** - Proper async handling for smooth performance
- 🔄 **Auto Updates** - Built-in update checking and notifications
- � **Rate Limiting** - Protection against spam and abuse

## 🎯 New in v1.3-BETA

- ✅ **Individual webhook controls** - Enable/disable each webhook type independently
- ✅ **Color code stripping** - Clean Discord messages without Minecraft formatting
- ✅ **Update notifications** - Automatic checking for new plugin versions
- ✅ **Enhanced Discord embeds** - Better formatting and information display
- ✅ **Improved error handling** - Better logging and fallback mechanisms

## 📋 Commands

### For Players
- `/report bug [description]` - Submit a new bug report with beautiful GUI
- `/reportbug [description]` - Legacy command (deprecated but still works)
  - Permission: `msnreports.report`

### For Staff
- `/managereports` (alias: `/mr`) - Main command for managing reports
  - `/mr list [page]` - View all reports with pagination
  - `/mr view <id>` - View detailed report information
  - `/mr status <id> <status>` - Update report status
  - `/mr comment <id> <message>` - Add comments to reports
  - `/mr filter <status> [page]` - Filter reports by status
  - `/mr reload` - Reload plugin configuration
  - Permission: `msnreports.manage`

## 🚀 Installation

1. Download the latest release from [GitHub Releases](https://github.com/msncakma/msnReports/releases)
2. Place the .jar file in your server's `plugins` folder
3. Restart your server
4. Configure Discord webhooks in `config.yml`
5. Set up database preferences (SQLite by default)

## ⚙️ Configuration

### Basic Setup
\`\`\`yaml
# License (required)
license: 'github.com/msncakma'

# Database configuration
database:
  type: 'sqlite' # or 'mysql'
  
# Discord webhook configuration
discord:
  enabled: true
  webhooks:
    reports:
      enabled: true
      url: 'YOUR_REPORTS_WEBHOOK_URL_HERE'
    admin-changes:
      enabled: true  
      url: 'YOUR_ADMIN_CHANGES_WEBHOOK_URL_HERE'
    admin-notes:
      enabled: true
      url: 'YOUR_ADMIN_NOTES_WEBHOOK_URL_HERE'
    status-changes:
      enabled: true
      url: 'YOUR_STATUS_CHANGES_WEBHOOK_URL_HERE'

# Update notifications
update-notifications:
  enabled: true
  check-on-startup: true
  notify-admins: true
\`\`\`

### MySQL Setup
\`\`\`yaml
database:
  type: 'mysql'
  mysql:
    host: 'localhost'
    port: 3306
    database: 'msnreports'
    username: 'your_username'
    password: 'your_password'
\`\`\`

## 🔑 Permissions

### Basic Permissions
- `msnreports.report` - Allow players to report bugs (default: true)
- `msnreports.manage` - Access to report management (default: op)
- `msnreports.notify` - Receive admin notifications (default: op)

### Advanced Permissions  
- `msnreports.manage.view` - View bug reports
- `msnreports.manage.status` - Change report status
- `msnreports.manage.comment` - Add comments to reports
- `msnreports.admin.reload` - Reload plugin configuration
- `msnreports.admin.notify` - Receive update notifications
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